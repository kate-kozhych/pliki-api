package com.event.plikiapi.analyzer;

import java.nio.file.Path;
import java.util.List;

public interface TextAnalyzer {
    Object analyzeSequential(List<Path> files) throws Exception;
    Object analyzeParallel(List<Path> files, int workers) throws Exception;
}
