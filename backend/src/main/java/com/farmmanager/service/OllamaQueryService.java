package com.farmmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OllamaQueryService {

    private static final Logger log = LoggerFactory.getLogger(OllamaQueryService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final KnowledgeBaseService knowledgeBaseService;

    private String schema = "";
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "qwen2.5-coder:7b";

    public OllamaQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, KnowledgeBaseService knowledgeBaseService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @PostConstruct
    public void loadSchema() {
        log.info("OllamaQueryService: Loading schema.sql dynamically...");
        try (InputStream is = new ClassPathResource("database/schema.sql").getInputStream()) {
            this.schema = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            log.info("OllamaQueryService: Dynamically cached schema ({} bytes).", schema.length());
        } catch (Exception e) {
            log.error("OllamaQueryService: Failed to dynamically load schema.sql", e);
            throw new RuntimeException("Failed to load schema.sql at startup", e);
        }
    }

    public static class OllamaResult {
        private final String sql;
        private final String answer;
        private final List<String> columns;
        private final List<Map<String, Object>> rows;

        public OllamaResult(String sql, String answer, List<String> columns, List<Map<String, Object>> rows) {
            this.sql = sql;
            this.answer = answer;
            this.columns = columns;
            this.rows = rows;
        }

        public String getSql() { return sql; }
        public String getAnswer() { return answer; }
        public List<String> getColumns() { return columns; }
        public List<Map<String, Object>> getRows() { return rows; }
    }

    /**
     * Coordinate NL -> SQL -> Database Execution -> Formatted Answer using local AI.
     */
    public OllamaResult processQuestion(String userQuestion) throws Exception {
        log.info("OllamaQueryService: Processing question: '{}'", userQuestion);

        // 1. Generate SQL from question via Ollama
        String rawSql = generateSql(userQuestion);
        String sql = cleanSql(rawSql);

        if (sql.isEmpty()) {
            throw new IllegalArgumentException("Ollama generated empty SQL.");
        }

        log.info("OllamaQueryService: Generated SQL: '{}'", sql);

        // 2. Validate SQL safety
        if (!isSqlSafe(sql)) {
            log.warn("OllamaQueryService: Security check failed. Rejected generated SQL: '{}'", sql);
            throw new SecurityException("Generated SQL failed safety check (Read-only SELECT/WITH queries only).");
        }

        // 3. Execute SQL against database
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        log.info("OllamaQueryService: Query executed successfully. Returned {} rows.", rows.size());

        // 4. Extract columns
        List<String> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }

        // 5. Format results to user-friendly answer string
        String answer = formatResult(rows);

        return new OllamaResult(sql, answer, columns, rows);
    }

    private String generateSql(String question) throws Exception {
        String prompt = buildPrompt(question);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP status code " + response.statusCode());
        }

        Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
        return (String) responseMap.get("response");
    }

    private String buildPrompt(String question) {
        String farmSoul = knowledgeBaseService.getFarmSoul();
        return "You are a SQLite SQL generator.\n\n" +
                "Farm Profile & Identity:\n" +
                farmSoul + "\n\n" +
                "Schema:\n" +
                schema + "\n\n" +
                "Rules:\n" +
                "* Return SQL only\n" +
                "* SQLite syntax only\n" +
                "* Read-only queries only\n" +
                "* Add LIMIT 100 where appropriate\n" +
                "* No explanations\n\n" +
                "Question: " + question + "\n" +
                "SQL:";
    }

    private String cleanSql(String response) {
        if (response == null) return "";
        String cleaned = response.trim();
        if (cleaned.contains("```sql")) {
            cleaned = cleaned.substring(cleaned.indexOf("```sql") + 6);
            if (cleaned.contains("```")) {
                cleaned = cleaned.substring(0, cleaned.indexOf("```"));
            }
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substring(cleaned.indexOf("```") + 3);
            if (cleaned.contains("```")) {
                cleaned = cleaned.substring(0, cleaned.indexOf("```"));
            }
        }
        cleaned = cleaned.trim();
        // Remove trailing semicolon if present
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        // Remove comment lines
        cleaned = Arrays.stream(cleaned.split("\n"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"))
                .trim();
        return cleaned;
    }

    public boolean isSqlSafe(String sql) {
        String trimmed = sql.trim().replaceAll("\\s+", " ").toUpperCase();
        
        // Safety Rule: must start with SELECT or WITH
        if (!trimmed.startsWith("SELECT ") && !trimmed.startsWith("WITH ")) {
            return false;
        }

        // Safety Rule: Reject structural modifiers or writes
        List<String> forbidden = List.of("DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE", "PRAGMA", "ATTACH");
        for (String word : forbidden) {
            Pattern pattern = Pattern.compile("\\b" + word + "\\b");
            if (pattern.matcher(trimmed).find()) {
                return false;
            }
        }

        return true;
    }

    private String formatResult(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No results found.";
        }

        // Case 1: Scalar result (1 row, 1 column)
        if (rows.size() == 1 && rows.get(0).size() == 1) {
            Map<String, Object> firstRow = rows.get(0);
            String colName = firstRow.keySet().iterator().next();
            Object val = firstRow.values().iterator().next();

            if (val == null) {
                return "0";
            }

            String colNameLower = colName.toLowerCase();
            String valStr = val.toString();

            // Prepend currency prefix for financial metrics
            if (colNameLower.contains("expense") || colNameLower.contains("cost") || colNameLower.contains("amount") || colNameLower.contains("revenue") || colNameLower.contains("transaction")) {
                try {
                    double d = Double.parseDouble(valStr);
                    return String.format("$%.2f", d);
                } catch (NumberFormatException e) {
                    return "$" + valStr;
                }
            }

            // Append unit labels for egg collection or milk yield
            if (colNameLower.contains("egg")) {
                return valStr + " eggs";
            }
            if (colNameLower.contains("milk") || colNameLower.contains("gallon") || colNameLower.contains("yield")) {
                return valStr + " gallons";
            }

            return valStr;
        }

        // Case 2: Multi-row or Multi-column results -> Format as simple readable bullet list
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            if (i > 0) sb.append("\n");
            sb.append("- ");
            int colIdx = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (colIdx > 0) sb.append(", ");
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
                colIdx++;
            }
        }
        return sb.toString();
    }
}
