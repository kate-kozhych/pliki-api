package com.event.plikiapi.service;

import com.event.plikiapi.analyzer.TextAnalyzer;
import com.event.plikiapi.analyzer.TopWordsAnalyzer;
import com.event.plikiapi.analyzer.TotalWordsAnalyzer;
import com.event.plikiapi.analyzer.UniqueWordsAnalyzer;
import com.event.plikiapi.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int[] WORKER_COUNTS = { 1, 2, 4, 8 };
    private static final int WARMUP_RUNS = 2;
    private static final int BENCHMARK_RUNS = 5;

    private final TotalWordsAnalyzer totalWordsAnalyzer;
    private final UniqueWordsAnalyzer uniqueWordsAnalyzer;
    private final TopWordsAnalyzer topWordsAnalyzer;

    private final ConcurrentHashMap<UUID, Task> store = new ConcurrentHashMap<>();
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(4);

    public Task create(TaskRequest request) {
        Task task = new Task(
                UUID.randomUUID(),
                request.getTaskType(),
                request.getInputPath(),
                request.getWorkers(),
                request.isRunBenchmark());
        store.put(task.getTaskId(), task);
        taskExecutor.submit(() -> execute(task));
        log.info("Task {} queued: {} on '{}'", task.getTaskId(), task.getTaskType(), task.getInputPath());
        return task;
    }

    public Task get(UUID id) {
        return store.get(id);
    }

    public List<Task> getAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    private void execute(Task task) {
        task.setStatus(TaskStatus.RUNNING);
        try {
            List<Path> files = loadFiles(task.getInputPath());
            if (files.isEmpty()) {
                throw new IllegalArgumentException("No .txt files found in: " + task.getInputPath());
            }
            log.info("Task {} processing {} files", task.getTaskId(), files.size());

            TextAnalyzer analyzer = resolveAnalyzer(task.getTaskType());

            if (task.isRunBenchmark()) {
                BenchmarkResult benchmark = benchmark(analyzer, files);
                task.setBenchmarkResult(benchmark);
            }

            Object result = analyzer.analyzeParallel(files, task.getWorkers());
            task.setResult(result);
            task.setStatus(TaskStatus.DONE);
            log.info("Task {} done", task.getTaskId());
        } catch (Exception e) {
            log.error("Task {} failed: {}", task.getTaskId(), e.getMessage());
            task.setErrorMessage(e.getMessage());
            task.setStatus(TaskStatus.FAILED);
        } finally {
            task.setCompletedAt(LocalDateTime.now());
        }
    }

    private BenchmarkResult benchmark(TextAnalyzer analyzer, List<Path> files) throws Exception {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            analyzer.analyzeSequential(files);
        }

        long seqTotal = 0;
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long t = System.currentTimeMillis();
            analyzer.analyzeSequential(files);
            seqTotal += System.currentTimeMillis() - t;
        }
        long seqAvg = seqTotal / BENCHMARK_RUNS;

        Map<Integer, Long> parallelTimes = new LinkedHashMap<>();
        for (int w : WORKER_COUNTS) {
            for (int i = 0; i < WARMUP_RUNS; i++) {
                analyzer.analyzeParallel(files, w);
            }
            long parTotal = 0;
            for (int i = 0; i < BENCHMARK_RUNS; i++) {
                long t = System.currentTimeMillis();
                analyzer.analyzeParallel(files, w);
                parTotal += System.currentTimeMillis() - t;
            }
            parallelTimes.put(w, parTotal / BENCHMARK_RUNS);
        }

        Map<Integer, Double> speedup = new LinkedHashMap<>();
        Map<Integer, Double> efficiency = new LinkedHashMap<>();
        parallelTimes.forEach((w, tN) -> {
            double s = seqAvg == 0 ? 1.0 : (double) seqAvg / tN;
            speedup.put(w, Math.round(s * 100.0) / 100.0);
            efficiency.put(w, Math.round((s / w) * 100.0) / 100.0);
        });

        return BenchmarkResult.builder()
                .sequentialTimeMs(seqAvg)
                .parallelTimesMs(parallelTimes)
                .speedup(speedup)
                .efficiency(efficiency)
                .build();
    }

    private TextAnalyzer resolveAnalyzer(TaskType type) {
        return switch (type) {
            case TOTAL_WORDS -> totalWordsAnalyzer;
            case UNIQUE_WORDS -> uniqueWordsAnalyzer;
            case TOP_WORDS -> topWordsAnalyzer;
        };
    }

    private List<Path> loadFiles(String inputPath) throws IOException {
        Path dir = Paths.get(inputPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory not found: " + inputPath);
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .collect(Collectors.toList());
        }
    }
}
