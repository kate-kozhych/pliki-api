package com.event.plikiapi.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Task {

    private final UUID taskId;
    private final TaskType taskType;
    private final String inputPath;
    private final int workers;
    private final boolean runBenchmark;
    private final LocalDateTime createdAt;

    private volatile TaskStatus status;
    private volatile Object result;
    private volatile String errorMessage;
    private volatile BenchmarkResult benchmarkResult;
    private volatile LocalDateTime completedAt;

    public Task(UUID taskId, TaskType taskType, String inputPath, int workers, boolean runBenchmark) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.inputPath = inputPath;
        this.workers = workers;
        this.runBenchmark = runBenchmark;
        this.status = TaskStatus.QUEUED;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getTaskId()                     { return taskId; }
    public TaskType getTaskType()               { return taskType; }
    public String getInputPath()                { return inputPath; }
    public int getWorkers()                     { return workers; }
    public boolean isRunBenchmark()             { return runBenchmark; }
    public LocalDateTime getCreatedAt()         { return createdAt; }

    public TaskStatus getStatus()               { return status; }
    public Object getResult()                   { return result; }
    public String getErrorMessage()             { return errorMessage; }
    public BenchmarkResult getBenchmarkResult() { return benchmarkResult; }
    public LocalDateTime getCompletedAt()       { return completedAt; }

    public void setStatus(TaskStatus status)              { this.status = status; }
    public void setResult(Object result)                  { this.result = result; }
    public void setErrorMessage(String errorMessage)      { this.errorMessage = errorMessage; }
    public void setBenchmarkResult(BenchmarkResult b)     { this.benchmarkResult = b; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
