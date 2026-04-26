package com.event.plikiapi.model;

import java.util.Map;

public record TopWordsResult(
    Map<String, Long> topWords,
    long executionTimeMs
) {}