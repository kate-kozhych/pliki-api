package com.event.plikiapi.model;

import lombok.Data;

@Data
public class TaskRequest {
    private TaskType taskType;
    private String inputPath = "texts/";
    private int workers = 4;
    private boolean runBenchmark = true;
}
