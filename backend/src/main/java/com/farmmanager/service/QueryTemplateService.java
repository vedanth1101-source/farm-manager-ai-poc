package com.farmmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class QueryTemplateService {

    private static final Logger log = LoggerFactory.getLogger(QueryTemplateService.class);
    private final JdbcTemplate jdbcTemplate;

    public QueryTemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Class to represent our matched result
    public static class QueryResult {
        private final String question;
        private final String sql;
        private final String answer;
        private final List<String> columns;
        private final List<Map<String, Object>> rows;

        public QueryResult(String question, String sql, String answer, List<String> columns, List<Map<String, Object>> rows) {
            this.question = question;
            this.sql = sql;
            this.answer = answer;
            this.columns = columns;
            this.rows = rows;
        }

        public String getQuestion() { return question; }
        public String getSql() { return sql; }
        public String getAnswer() { return answer; }
        public List<String> getColumns() { return columns; }
        public List<Map<String, Object>> getRows() { return rows; }
    }

    // Helper class for our template matching definitions
    private static class QueryTemplate {
        int id;
        Pattern pattern;
        String standardQuestion;
        String sql;
        ResultFormatter formatter;

        QueryTemplate(int id, String regex, String standardQuestion, String sql, ResultFormatter formatter) {
            this.id = id;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.standardQuestion = standardQuestion;
            this.sql = sql;
            this.formatter = formatter;
        }
    }

    // Functional interface to format dynamic rows into readable text
    @FunctionalInterface
    private interface ResultFormatter {
        String format(List<Map<String, Object>> rows);
    }

    private final List<QueryTemplate> templates = new ArrayList<>();

    // Initialize all 10 templates
    {
        // 1. How many eggs did I collect last week?
        templates.add(new QueryTemplate(
            1,
            ".*eggs.*(last week|7 days).*",
            "How many eggs did I collect last week?",
            "SELECT SUM(quantity) AS total_eggs FROM egg_collection WHERE collection_date >= date('now', '-7 days')",
            rows -> {
                if (rows.isEmpty() || rows.get(0).get("total_eggs") == null) return "0 eggs collected last week.";
                return rows.get(0).get("total_eggs").toString() + " eggs";
            }
        ));

        // 2. How many eggs did I collect this month?
        templates.add(new QueryTemplate(
            2,
            ".*eggs.*(this month|current month).*",
            "How many eggs did I collect this month?",
            "SELECT SUM(quantity) AS total_eggs FROM egg_collection WHERE collection_date >= date('now', 'start of month')",
            rows -> {
                if (rows.isEmpty() || rows.get(0).get("total_eggs") == null) return "0 eggs collected this month.";
                return rows.get(0).get("total_eggs").toString() + " eggs";
            }
        ));

        // 3. Which chickens produced the most eggs?
        templates.add(new QueryTemplate(
            3,
            ".*chicken.*(most eggs|highest egg|produced most).*",
            "Which chickens produced the most eggs?",
            """
            SELECT a.name, a.breed, SUM(e.quantity) AS total_eggs
            FROM animals a
            JOIN egg_collection e ON a.id = e.animal_id
            WHERE a.species = 'Chicken'
            GROUP BY a.id, a.name, a.breed
            ORDER BY total_eggs DESC
            """,
            rows -> {
                if (rows.isEmpty()) return "No laying chickens found.";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(rows.size(), 3); i++) {
                    Map<String, Object> r = rows.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(r.get("name")).append(" (").append(r.get("total_eggs")).append(" eggs)");
                }
                return sb.toString();
            }
        ));

        // 4. What was my feed expense last month?
        templates.add(new QueryTemplate(
            4,
            ".*feed expense.*last month.*",
            "What was my feed expense last month?",
            """
            SELECT SUM(amount) AS total_feed_expense
            FROM expenses
            WHERE category = 'Feed'
              AND expense_date >= date('now', 'start of month', '-1 month')
              AND expense_date < date('now', 'start of month')
            """,
            rows -> {
                if (rows.isEmpty() || rows.get(0).get("total_feed_expense") == null) return "$0.00 feed expense last month.";
                return "$" + String.format("%.2f", ((Number) rows.get(0).get("total_feed_expense")).doubleValue());
            }
        ));

        // 5. What were my total expenses last month?
        templates.add(new QueryTemplate(
            5,
            ".*total expenses.*last month.*",
            "What were my total expenses last month?",
            """
            SELECT SUM(amount) AS total_expenses
            FROM expenses
            WHERE expense_date >= date('now', 'start of month', '-1 month')
              AND expense_date < date('now', 'start of month')
            """,
            rows -> {
                if (rows.isEmpty() || rows.get(0).get("total_expenses") == null) return "$0.00 total expenses last month.";
                return "$" + String.format("%.2f", ((Number) rows.get(0).get("total_expenses")).doubleValue());
            }
        ));

        // 6. Which rabbits gained the most weight?
        templates.add(new QueryTemplate(
            6,
            ".*rabbit.*(most weight|gained weight|weight gain).*",
            "Which rabbits gained the most weight?",
            """
            SELECT name, breed, starting_weight_lbs, current_weight_lbs,
                   round(current_weight_lbs - starting_weight_lbs, 2) AS weight_gain_lbs
            FROM animals
            WHERE species = 'Rabbit'
            ORDER BY weight_gain_lbs DESC
            """,
            rows -> {
                if (rows.isEmpty()) return "No rabbit weight records found.";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(rows.size(), 2); i++) {
                    Map<String, Object> r = rows.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(r.get("name"))
                      .append(" (+")
                      .append(String.format("%.2f", ((Number) r.get("weight_gain_lbs")).doubleValue()))
                      .append(" lbs, ")
                      .append(r.get("starting_weight_lbs"))
                      .append(" to ")
                      .append(r.get("current_weight_lbs"))
                      .append(" lbs)");
                }
                return sb.toString();
            }
        ));

        // 7. Show health incidents in the last 30 days.
        templates.add(new QueryTemplate(
            7,
            ".*(health incidents|health records|incident).*last 30 days.*",
            "Show health incidents in the last 30 days.",
            """
            SELECT a.name AS animal_name, a.species, h.incident_date, h.diagnosis, h.treatment, h.cost
            FROM health_records h
            JOIN animals a ON h.animal_id = a.id
            WHERE h.incident_date >= date('now', '-30 days')
            ORDER BY h.incident_date DESC
            """,
            rows -> {
                if (rows.isEmpty()) return "0 health incidents recorded in the last 30 days.";
                StringBuilder sb = new StringBuilder();
                sb.append(rows.size()).append(" health incident").append(rows.size() > 1 ? "s" : "").append(" found: ");
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> r = rows.get(i);
                    if (i > 0) sb.append("; ");
                    sb.append(r.get("diagnosis"))
                      .append(" for ")
                      .append(r.get("species"))
                      .append(" ")
                      .append(r.get("animal_name"))
                      .append(" on ")
                      .append(r.get("incident_date"))
                      .append(" ($")
                      .append(String.format("%.2f", ((Number) r.get("cost")).doubleValue()))
                      .append(")");
                }
                return sb.toString();
            }
        ));

        // 8. Which animals need attention?
        templates.add(new QueryTemplate(
            8,
            ".*animals.*need attention.*",
            "Which animals need attention?",
            """
            SELECT id, name, species, breed, status,
                   CASE
                     WHEN status = 'Requires Attention' THEN 'Flagged for attention'
                     WHEN id IN (SELECT DISTINCT animal_id FROM health_records WHERE status = 'Active') THEN 'Active health incident'
                     WHEN current_weight_lbs < starting_weight_lbs THEN 'Weight loss detected'
                     ELSE 'Normal'
                   END AS attention_reason
            FROM animals
            WHERE status = 'Requires Attention'
               OR id IN (SELECT DISTINCT animal_id FROM health_records WHERE status = 'Active')
               OR current_weight_lbs < starting_weight_lbs
            """,
            rows -> {
                if (rows.isEmpty()) return "All animals are in excellent health! Zero anomalies detected.";
                StringBuilder sb = new StringBuilder();
                sb.append(rows.size()).append(" animal").append(rows.size() > 1 ? "s require" : " requires").append(" attention: ");
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> r = rows.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(r.get("name"))
                      .append(" (")
                      .append(r.get("species"))
                      .append(" - ")
                      .append(r.get("attention_reason"))
                      .append(")");
                }
                return sb.toString();
            }
        ));

        // 9. Show milk production this month.
        templates.add(new QueryTemplate(
            9,
            ".*milk production.*this month.*",
            "Show milk production this month.",
            """
            SELECT a.name, a.breed, round(SUM(m.yield_gallons), 1) AS total_milk_gallons
            FROM animals a
            JOIN milk_production m ON a.id = m.animal_id
            WHERE a.species = 'Cow'
              AND m.milking_date >= date('now', 'start of month')
            GROUP BY a.id, a.name, a.breed
            ORDER BY total_milk_gallons DESC
            """,
            rows -> {
                if (rows.isEmpty()) return "0 gallons of milk production logged this month.";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> r = rows.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(r.get("name")).append(" (").append(r.get("total_milk_gallons")).append(" gal)");
                }
                return sb.toString();
            }
        ));

        // 10. Compare egg production month-over-month.
        templates.add(new QueryTemplate(
            10,
            ".*(compare egg production|egg production MoM|egg production month-over-month).*",
            "Compare egg production month-over-month.",
            """
            SELECT
              strftime('%Y-%m', collection_date) AS month,
              SUM(quantity) AS total_eggs,
              COUNT(DISTINCT collection_date) AS active_laying_days
            FROM egg_collection
            WHERE collection_date >= date('now', '-6 months')
            GROUP BY month
            ORDER BY month DESC
            """,
            rows -> {
                if (rows.isEmpty()) return "No laying history available for month-over-month comparison.";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(rows.size(), 3); i++) {
                    Map<String, Object> r = rows.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(r.get("month")).append(": ").append(r.get("total_eggs")).append(" eggs");
                }
                return sb.toString();
            }
        ));
    }

    /**
     * Match a natural language question against our pre-defined templates.
     * Executes the query on success and formats the answer dynamically.
     */
    public QueryResult processQuestion(String userQuestion) {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return new QueryResult(
                "Empty query",
                "",
                "Please type a valid farm question.",
                Collections.emptyList(),
                Collections.emptyList()
            );
        }

        String cleaned = userQuestion.trim();

        for (QueryTemplate template : templates) {
            if (template.pattern.matcher(cleaned).matches()) {
                log.info("Matched question template ID {}: '{}'", template.id, template.standardQuestion);
                try {
                    // Execute raw SQL using Spring JdbcTemplate
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(template.sql);

                    // Extract columns dynamically from the first row of results
                    List<String> columns = new ArrayList<>();
                    if (!rows.isEmpty()) {
                        columns.addAll(rows.get(0).keySet());
                    }

                    // Format the user-friendly description using our custom lambda formatter
                    String answer = template.formatter.format(rows);

                    return new QueryResult(
                        template.standardQuestion,
                        template.sql,
                        answer,
                        columns,
                        rows
                    );
                } catch (Exception e) {
                    log.error("Error executing query for template ID {}: {}", template.id, e.getMessage(), e);
                    return new QueryResult(
                        template.standardQuestion,
                        template.sql,
                        "Error executing SQLite query: " + e.getMessage(),
                        Collections.singletonList("Error"),
                        Collections.singletonList(Map.of("Error", e.getMessage()))
                    );
                }
            }
        }

        // Default response when no template is matched
        log.warn("Failed to match question pattern: '{}'", cleaned);
        return new QueryResult(
            cleaned,
            "-- No template matched for this question",
            "Sorry, I couldn't match your question to any of the 10 pre-defined templates. Try clicking one of the suggested templates below for a reliable demonstration!",
            Collections.singletonList("Status"),
            Collections.singletonList(Map.of("Message", "Could not map query to template"))
        );
    }

    public List<String> getSupportedQuestions() {
        List<String> list = new ArrayList<>();
        for (QueryTemplate t : templates) {
            list.add(t.standardQuestion);
        }
        return list;
    }
}
