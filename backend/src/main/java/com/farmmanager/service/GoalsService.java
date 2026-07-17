package com.farmmanager.service;

import com.farmmanager.dto.GoalDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GoalsService {

    private static final Logger log = LoggerFactory.getLogger(GoalsService.class);
    private final JdbcTemplate jdbcTemplate;
    private final OllamaQueryService ollamaQueryService;

    public GoalsService(JdbcTemplate jdbcTemplate, OllamaQueryService ollamaQueryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.ollamaQueryService = ollamaQueryService;
    }

    public List<GoalDTO> getEvaluatedGoals() {
        log.info("GoalsService: Fetching and evaluating all goals...");
        List<GoalDTO> goals = getAllGoals();
        for (GoalDTO goal : goals) {
            evaluateGoal(goal);
        }
        return goals;
    }

    private List<GoalDTO> getAllGoals() {
        String sql = "SELECT id, name, target_value, current_value, sql_metric_query, target_date, status FROM goals";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GoalDTO> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            list.add(new GoalDTO(
                    (Integer) row.get("id"),
                    (String) row.get("name"),
                    ((Number) row.get("target_value")).doubleValue(),
                    row.get("current_value") != null ? ((Number) row.get("current_value")).doubleValue() : 0.0,
                    (String) row.get("sql_metric_query"),
                    (String) row.get("target_date"),
                    (String) row.get("status")
            ));
        }
        return list;
    }

    public void evaluateGoal(GoalDTO goal) {
        try {
            log.info("Evaluating goal: '{}' with query: '{}'", goal.getName(), goal.getSqlMetricQuery());
            if (goal.getSqlMetricQuery() != null && !ollamaQueryService.isSqlSafe(goal.getSqlMetricQuery())) {
                log.warn("Skipping evaluation of goal '{}': SQL query failed safety check.", goal.getName());
                String updateSql = "UPDATE goals SET status = ? WHERE id = ?";
                jdbcTemplate.update(updateSql, "Failed", goal.getId());
                goal.setStatus("Failed");
                return;
            }

            Double computedValue = jdbcTemplate.queryForObject(goal.getSqlMetricQuery(), Double.class);
            if (computedValue == null) {
                computedValue = 0.0;
            }

            goal.setCurrentValue(computedValue);
            String newStatus = "Active";
            if (goal.getName().toLowerCase().contains("limit") || goal.getName().toLowerCase().contains("expense")) {
                // If it is a cost limit goal
                if (computedValue > goal.getTargetValue()) {
                    newStatus = "Missed";
                } else {
                    newStatus = "Active"; // Or Met if deadline passed, but for now we keep it Active
                }
            } else {
                // For yield/quantity target goals
                if (computedValue >= goal.getTargetValue()) {
                    newStatus = "Met";
                }
            }
            goal.setStatus(newStatus);

            // Persist evaluation back to SQLite database
            String updateSql = "UPDATE goals SET current_value = ?, status = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, computedValue, newStatus, goal.getId());
            log.info("Goal evaluated: '{}' updated to current_value: {}, status: {}", goal.getName(), computedValue, newStatus);
        } catch (Exception e) {
            log.error("Failed to evaluate goal '{}': {}", goal.getName(), e.getMessage());
        }
    }

    public void addGoal(GoalDTO goal) {
        log.info("GoalsService: Creating new goal: '{}'", goal.getName());
        if (goal.getSqlMetricQuery() != null && !ollamaQueryService.isSqlSafe(goal.getSqlMetricQuery())) {
            log.error("Failed to add goal '{}': SQL query failed safety check.", goal.getName());
            throw new SecurityException("Goal SQL query failed safety check (Read-only SELECT/WITH queries only).");
        }
        String insertSql = "INSERT INTO goals (name, target_value, sql_metric_query, target_date, status) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, goal.getName(), goal.getTargetValue(), goal.getSqlMetricQuery(), goal.getTargetDate(), "Active");
    }
}
