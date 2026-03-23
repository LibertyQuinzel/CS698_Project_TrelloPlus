package com.flowboard.dto;

import java.util.List;
import java.util.Map;

public class AIAnalysisResult {
    private List<StageInfo> stages;
    private List<TaskInfo> tasks;

    public static class StageInfo {
        public String title;
        public String color;
        public int position;
    }

    public static class TaskInfo {
        public String title;
        public String description;
        public String priority;
        public String stageTitle;
    }

    public List<StageInfo> getStages() {
        return stages;
    }

    public void setStages(List<StageInfo> stages) {
        this.stages = stages;
    }

    public List<TaskInfo> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskInfo> tasks) {
        this.tasks = tasks;
    }
}
