package com.farmmanager.dto;

import java.time.LocalDate;

public class GoalDTO {
    private int id;
    private String name;
    private double targetValue;
    private double currentValue;
    private String sqlMetricQuery;
    private String targetDate;
    private String status;

    public GoalDTO() {}

    public GoalDTO(int id, String name, double targetValue, double currentValue, String sqlMetricQuery, String targetDate, String status) {
        this.id = id;
        this.name = name;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.sqlMetricQuery = sqlMetricQuery;
        this.targetDate = targetDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public String getSqlMetricQuery() { return sqlMetricQuery; }
    public void setSqlMetricQuery(String sqlMetricQuery) { this.sqlMetricQuery = sqlMetricQuery; }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
