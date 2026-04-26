package com.event.plikiapi.analyzer;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class UniqueWordsAnalyzer implements TextAnalyzer {

    @Override
    public Long analyzeSequential(List<Path> files) throws Exception {
        Set<String> uniqueWords = new HashSet<>();
        for (Path file : files) {
            extractUniqueWords(file, uniqueWords);
        }
        return (long) uniqueWords.size();
    }

    @Override
    public Long analyzeParallel(List<Path> files, int workers) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        Set<String> uniqueWords = ConcurrentHashMap.newKeySet();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Path file : files) {
                futures.add(executor.submit(() -> {
                    try {
                        extractUniqueWords(file, uniqueWords);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
            return (long) uniqueWords.size();
        } finally {
            executor.shutdown();
        }
    }

    private void extractUniqueWords(Path file, Set<String> targetSet) throws IOException {
        String content = Files.readString(file);
        if (content.isBlank()) return;
        
        String normalized = content.toLowerCase().replaceAll("\\p{Punct}", "");
        String[] tokens = normalized.trim().split("\\s+");
        for (String token : tokens) {
            if (!token.isBlank()) {
                targetSet.add(token);
            }
        }
    }
}
