package com.event.plikiapi.analyzer;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class TotalWordsAnalyzer implements TextAnalyzer {

    @Override
    public Long analyzeSequential(List<Path> files) throws Exception {
        long total = 0;
        for (Path file : files) {
            total += countWords(file);
        }
        return total;
    }

    @Override
    public Long analyzeParallel(List<Path> files, int workers) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (Path file : files) {
                futures.add(executor.submit(() -> countWords(file)));
            }
            long total = 0;
            for (Future<Long> future : futures) {
                total += future.get();
            }
            return total;
        } finally {
            executor.shutdown();
        }
    }

    private long countWords(Path file) throws IOException {
        String content = Files.readString(file);
        if (content.isBlank()) return 0;
        String[] tokens = content.trim().split("\\s+"); // \\s+ one or more whitespace characters (space, tab, newline, etc.)
        return tokens.length;
    }
}
