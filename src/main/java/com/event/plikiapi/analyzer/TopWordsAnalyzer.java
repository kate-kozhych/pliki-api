package com.event.plikiapi.analyzer;

import com.event.plikiapi.model.TopWordsResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TopWordsAnalyzer implements TextAnalyzer {

    private static final int TOP_N = 10;

    @Override
    public Object analyzeSequential(List<Path> files) {
        long startTime = System.currentTimeMillis();
        Map<String, Long> totalCounts = new HashMap<>();

        for (Path file : files) {
            try (Stream<String> lines = Files.lines(file)) {
                lines.flatMap(line -> Arrays.stream(line.toLowerCase().split("\\W+")))
                        .filter(word -> !word.isEmpty())
                        .forEach(word -> totalCounts.merge(word, 1L, Long::sum));
            } catch (IOException e) {
                System.err.println("Reading file error: " + file.getFileName());
            }
        }

        Map<String, Long> sortedTopWords = getTopN(totalCounts, TOP_N);
        long endTime = System.currentTimeMillis();

        return new TopWordsResult(sortedTopWords, (endTime - startTime));
    }

    @Override
    public Object analyzeParallel(List<Path> files, int workers) {
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        ConcurrentHashMap<String, LongAdder> concurrentCounts = new ConcurrentHashMap<>();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (Path file : files) {
            tasks.add(() -> {
                try (Stream<String> lines = Files.lines(file)) {
                    lines.flatMap(line -> Arrays.stream(line.toLowerCase().split("\\W+")))
                            .filter(word -> !word.isEmpty())
                            .forEach(word -> concurrentCounts
                                    .computeIfAbsent(word, k -> new LongAdder())
                                    .increment());
                } catch (IOException e) {
                    System.err.println("Reading file error: " + file.getFileName());
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel execution was interrupted", e);
        } finally {
            executor.shutdown();
        }

        Map<String, Long> finalCounts = new HashMap<>();
        concurrentCounts.forEach((k, v) -> finalCounts.put(k, v.sum()));

        Map<String, Long> sortedTopWords = getTopN(finalCounts, TOP_N);
        long endTime = System.currentTimeMillis();

        return new TopWordsResult(sortedTopWords, (endTime - startTime));
    }

    private Map<String, Long> getTopN(Map<String, Long> counts, int n) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (Long e1, Long e2) -> e1,
                        LinkedHashMap::new));
    }
}