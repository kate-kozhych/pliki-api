package com.event.plikiapi.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BenchmarkResult {
    private long sequentialTimeMs;
    private Map<Integer, Long> parallelTimesMs;
    private Map<Integer, Double> speedup;
    private Map<Integer, Double> efficiency;
}
