package com.event.plikiapi.analyzer;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Component
public class TopWordsAnalyzer implements TextAnalyzer {

    private static final int TOP_N = 10;

    @Override
    public Map<String, Long> analyzeSequential(List<Path> files) throws Exception {
        Map<String, Long> totalCounts = new HashMap<>();

        for (Path file : files) {
            countWordsSequential(file, totalCounts);
        }

        return getTopN(totalCounts, TOP_N);
    }

    @Override
    public Map<String, Long> analyzeParallel(List<Path> files, int workers) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        ConcurrentHashMap<String, LongAdder> concurrentCounts = new ConcurrentHashMap<>();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Path file : files) {
                futures.add(executor.submit(() -> {
                    countWordsParallel(file, concurrentCounts);
                    return null;
                }));
            }

            for (Future<Void> future : futures) {
                future.get();
            }

            Map<String, Long> finalCounts = new HashMap<>();
            concurrentCounts.forEach((k, v) -> finalCounts.put(k, v.sum()));

            return getTopN(finalCounts, TOP_N);
        } finally {
            executor.shutdown();
        }
    }

    private void countWordsSequential(Path file, Map<String, Long> counts) throws IOException {
        String content = Files.readString(file);
        if (content.isBlank())
            return;

        String[] tokens = content.trim().toLowerCase().split("\\W+");
        for (String word : tokens) {
            if (!word.isEmpty()) {
                counts.put(word, counts.getOrDefault(word, 0L) + 1L);
            }
        }
    }

    private void countWordsParallel(Path file, ConcurrentHashMap<String, LongAdder> concurrentCounts)
            throws IOException {
        String content = Files.readString(file);
        if (content.isBlank())
            return;

        String[] tokens = content.trim().toLowerCase().split("\\W+");
        for (String word : tokens) {
            if (!word.isEmpty()) {
                concurrentCounts.computeIfAbsent(word, k -> new LongAdder()).increment();
            }
        }
    }

    private Map<String, Long> getTopN(Map<String, Long> counts, int n) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}