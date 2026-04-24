package com.event.plikiapi.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private UUID taskId;
    private TaskType taskType;
    private TaskStatus status;
    private String inputPath;
    private int workers;
    private Object result;
    private String errorMessage;
    private BenchmarkResult benchmarkResult;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
