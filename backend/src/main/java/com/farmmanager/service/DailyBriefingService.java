package com.farmmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DailyBriefingService {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefingService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private String cachedBriefing = "";
    private long lastGeneratedTime = 0;
    private static final long CACHE_TTL_MS = 600000; // 10 minutes cache TTL

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "qwen2.5-coder:7b";

    public DailyBriefingService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public synchronized String getDailyBriefing() {
        long now = System.currentTimeMillis();
        if (cachedBriefing.isEmpty() || (now - lastGeneratedTime > CACHE_TTL_MS)) {
            log.info("DailyBriefingService: Briefing cache expired or empty. Regenerating...");
            cachedBriefing = generateBriefing();
            lastGeneratedTime = now;
        } else {
            log.info("DailyBriefingService: Serving briefing from cache.");
        }
        return cachedBriefing;
    }

    public synchronized void forceRegenerate() {
        log.info("DailyBriefingService: Force regenerating briefing...");
        cachedBriefing = generateBriefing();
        lastGeneratedTime = System.currentTimeMillis();
    }

    private String generateBriefing() {
        try {
            // 1. Gather Database Metrics
            int totalEggsLastWeek = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(quantity), 0) FROM egg_collection WHERE collection_date >= date('now', '-7 days')", Integer.class);

            double totalMilkThisMonth = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(yield_gallons), 0) FROM milk_production WHERE milking_date >= date('now', 'start of month')", Double.class);

            int activeHealthIssues = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM health_records WHERE status = 'Active'", Integer.class);

            List<Map<String, Object>> healthIssues = jdbcTemplate.queryForList(
                    "SELECT diagnosis, treatment FROM health_records WHERE status = 'Active'");

            double totalExpensesThisMonth = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type='Expense' AND transaction_date >= date('now', 'start of month')", Double.class);

            List<Map<String, Object>> expensesByCategory = jdbcTemplate.queryForList(
                    "SELECT category, SUM(amount) as total FROM transactions WHERE type='Expense' AND transaction_date >= date('now', 'start of month') GROUP BY category");

            // 2. Format local stats string
            StringBuilder stats = new StringBuilder();
            stats.append(String.format("1. Egg collections (last 7 days): %d eggs\n", totalEggsLastWeek));
            stats.append(String.format("2. Milk production (this month): %.1f gallons\n", totalMilkThisMonth));
            stats.append(String.format("3. Active animal health issues: %d\n", activeHealthIssues));
            for (Map<String, Object> issue : healthIssues) {
                stats.append(String.format("   - Diagnosis: %s, Treatment: %s\n", issue.get("diagnosis"), issue.get("treatment")));
            }
            stats.append(String.format("4. Monthly Expenses: $%.2f\n", totalExpensesThisMonth));
            for (Map<String, Object> exp : expensesByCategory) {
                stats.append(String.format("   - %s: $%.2f\n", exp.get("category"), ((Number) exp.get("total")).doubleValue()));
            }

            // 3. Try to call Ollama for a rich briefing
            try {
                String prompt = "You are a professional farming operations analyst and general assistant. " +
                        "Generate a concise, daily operational briefing for a farm operator based on these database statistics:\n\n" +
                        stats.toString() + "\n" +
                        "Requirements:\n" +
                        "* Keep it to 2-3 short, actionable paragraphs.\n" +
                        "* Do not use markdown headers (##), only simple bold text and bullet points.\n" +
                        "* Highlight any problems or cost anomalies immediately (e.g., active health records require attention, spending bounds).\n" +
                        "* Provide a welcoming, professional greeting like 'Good morning, Operator.'";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", MODEL_NAME);
                requestBody.put("prompt", prompt);
                requestBody.put("stream", false);

                String jsonRequest = objectMapper.writeValueAsString(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(OLLAMA_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                        .timeout(Duration.ofSeconds(12))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
                    return (String) responseMap.get("response");
                }
            } catch (Exception e) {
                log.warn("Ollama briefing generation failed or timed out: {}. Using template fallback...", e.getMessage());
            }

            // 4. Fallback: Generate a structured briefing template if Ollama is down
            StringBuilder fallback = new StringBuilder();
            fallback.append("Good morning, Operator. Here is your deterministic Daily Briefing.\n\n");
            fallback.append(String.format("**Livestock yields**: Chickens produced **%d eggs** in the last 7 days. Cows produced **%.1f gallons** of milk so far this month.\n\n", totalEggsLastWeek, totalMilkThisMonth));
            if (activeHealthIssues > 0) {
                fallback.append(String.format("**Alert - Animal Health**: There are currently **%d active health issues** requiring attention:\n", activeHealthIssues));
                for (Map<String, Object> issue : healthIssues) {
                    fallback.append(String.format("*   Diagnosis: _%s_ (Treatment: %s)\n", issue.get("diagnosis"), issue.get("treatment")));
                }
                fallback.append("\n");
            } else {
                fallback.append("**Livestock Health**: No active veterinary or health incidents reported. All animals are active.\n\n");
            }
            fallback.append(String.format("**Financial Position**: Total expenses recorded this month are **$%.2f**.", totalExpensesThisMonth));
            return fallback.toString();

        } catch (Exception e) {
            log.error("Failed to build daily briefing: {}", e.getMessage(), e);
            return "Good morning, Operator. Unable to load farm statistics from database. Please check SQLite connection.";
        }
    }
}
