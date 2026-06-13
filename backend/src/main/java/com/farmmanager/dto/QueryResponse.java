package com.farmmanager.dto;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    private String question;
    private String sql;
    private String answer;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private String mode;

    public QueryResponse() {}

    public QueryResponse(String question, String sql, String answer, List<String> columns, List<Map<String, Object>> rows, String mode) {
        this.question = question;
        this.sql = sql;
        this.answer = answer;
        this.columns = columns;
        this.rows = rows;
        this.mode = mode;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
