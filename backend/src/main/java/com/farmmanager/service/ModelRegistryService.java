package com.farmmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ModelRegistryService {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistryService.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private static final String OLLAMA_TAGS_URL = "http://localhost:11434/api/tags";

    // Task to Model routing map
    private final Map<String, String> routingMap = new ConcurrentHashMap<>();
    
    // Usage/Cost records
    private final List<CostRecord> costRecords = new CopyOnWriteArrayList<>();

    public ModelRegistryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        // Initialize default routing
        routingMap.put("ANOMALY_DETECTOR", "qwen2.5-coder:7b");
        routingMap.put("MARKET_ANALYZER", "qwen2.5-coder:7b");
        routingMap.put("WEB_INTELLIGENCE", "gemma3:4b");
        routingMap.put("OUTREACH_SPECIALIST", "deepseek-r1:8b");
        routingMap.put("IMAGE_ANALYZER", "llava:latest");
        routingMap.put("MODEL_ROUTER", "qwen2.5-coder:7b");
    }

    @PostConstruct
    public void init() {
        log.info("ModelRegistryService: Initialized. Fetching available models...");
        List<String> available = getAvailableModels();
        log.info("ModelRegistryService: Available models on startup: {}", available);
        
        // Ensure routed models are actually available, otherwise map to first available
        if (!available.isEmpty()) {
            for (Map.Entry<String, String> entry : routingMap.entrySet()) {
                if (!available.contains(entry.getValue())) {
                    // special fallback logic: if qwen2.5-coder:7b is routed and available, or deepseek/gemma, etc.
                    String fallback = available.contains("qwen2.5-coder:7b") ? "qwen2.5-coder:7b" : available.get(0);
                    // For image analyzer, we keep llava:latest as configured, but we will handle it gracefully in execution
                    if (!"IMAGE_ANALYZER".equals(entry.getKey())) {
                        routingMap.put(entry.getKey(), fallback);
                    }
                }
            }
        }
    }

    public List<String> getAvailableModels() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_TAGS_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<?, ?> bodyMap = objectMapper.readValue(response.body(), Map.class);
                List<?> modelsList = (List<?>) bodyMap.get("models");
                List<String> models = new ArrayList<>();
                if (modelsList != null) {
                    for (Object m : modelsList) {
                        if (m instanceof Map) {
                            String name = (String) ((Map<?, ?>) m).get("name");
                            if (name != null) {
                                models.add(name);
                            }
                        }
                    }
                }
                return models;
            }
        } catch (Exception e) {
            log.warn("ModelRegistryService: Failed to retrieve models from Ollama API. Error: {}", e.getMessage());
        }
        // Graceful fallback to static list of expected models in environment
        return Arrays.asList("qwen2.5-coder:7b", "deepseek-r1:8b", "gemma3:4b", "qwen3:8b", "gemma4:latest", "qwen2.5-coder:14b", "gpt-oss:20b");
    }

    public Map<String, String> getRoutingMap() {
        return new HashMap<>(routingMap);
    }

    public void updateRouting(String taskName, String modelName) {
        if (taskName != null && modelName != null) {
            routingMap.put(taskName.toUpperCase(), modelName);
            log.info("ModelRegistryService: Updated routing for {} to {}", taskName, modelName);
        }
    }

    public String getModelForTask(String taskName) {
        String routedModel = routingMap.get(taskName.toUpperCase());
        if (routedModel == null) {
            routedModel = "qwen2.5-coder:7b"; // default fallback
        }
        
        // Validate if model is available, otherwise pick a valid fallback
        List<String> available = getAvailableModels();
        if (!available.isEmpty() && !"llava:latest".equals(routedModel) && !available.contains(routedModel)) {
            // Pick qwen2.5-coder:7b if available, otherwise first available
            if (available.contains("qwen2.5-coder:7b")) {
                return "qwen2.5-coder:7b";
            }
            return available.get(0);
        }
        return routedModel;
    }

    public void recordUsage(String taskId, String taskName, String modelName, long promptTokens, long completionTokens) {
        double cost = calculateCost(modelName, promptTokens, completionTokens);
        CostRecord record = new CostRecord(taskId, taskName, modelName, promptTokens, completionTokens, cost, LocalDateTime.now());
        costRecords.add(record);
        log.info("ModelRegistryService: Recorded usage for task {} ({}): {} tokens, cost: ${}", taskId, modelName, promptTokens + completionTokens, String.format("%.6f", cost));
    }

    private double calculateCost(String modelName, long promptTokens, long completionTokens) {
        // Prices per million tokens (cloud API equivalents for analytics)
        double promptRate = 0.15; // default $0.15/1M
        double completionRate = 0.60; // default $0.60/1M

        String lowerModel = modelName.toLowerCase();
        if (lowerModel.contains("qwen2.5-coder:14b")) {
            promptRate = 0.30;
            completionRate = 1.20;
        } else if (lowerModel.contains("qwen2.5-coder:7b") || lowerModel.contains("qwen3:8b") || lowerModel.contains("deepseek-r1:8b")) {
            promptRate = 0.15;
            completionRate = 0.60;
        } else if (lowerModel.contains("gemma3:4b")) {
            promptRate = 0.08;
            completionRate = 0.32;
        } else if (lowerModel.contains("gemma4:latest")) {
            promptRate = 0.20;
            completionRate = 0.80;
        } else if (lowerModel.contains("gpt-oss:20b")) {
            promptRate = 0.50;
            completionRate = 2.00;
        } else if (lowerModel.contains("llava")) {
            promptRate = 0.20;
            completionRate = 0.80;
        }

        double pCost = (promptTokens / 1_000_000.0) * promptRate;
        double cCost = (completionTokens / 1_000_000.0) * completionRate;
        return pCost + cCost;
    }

    public Map<String, Object> getCostReport() {
        double totalCost = 0.0;
        long totalTokens = 0;
        
        Map<String, TaskAgg> taskAggs = new HashMap<>();
        Map<String, ModelAgg> modelAggs = new HashMap<>();
        List<Map<String, Object>> historyList = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (CostRecord rec : costRecords) {
            totalCost += rec.cost;
            totalTokens += (rec.promptTokens + rec.completionTokens);

            // Task Aggregation
            taskAggs.computeIfAbsent(rec.taskName, k -> new TaskAgg(rec.taskName)).add(rec.cost, rec.promptTokens + rec.completionTokens);

            // Model Aggregation
            modelAggs.computeIfAbsent(rec.modelName, k -> new ModelAgg(rec.modelName)).add(rec.cost, rec.promptTokens + rec.completionTokens);

            // History format
            Map<String, Object> hist = new HashMap<>();
            hist.put("taskId", rec.taskId);
            hist.put("taskName", rec.taskName);
            hist.put("modelName", rec.modelName);
            hist.put("promptTokens", rec.promptTokens);
            hist.put("completionTokens", rec.completionTokens);
            hist.put("totalTokens", rec.promptTokens + rec.completionTokens);
            hist.put("cost", rec.cost);
            hist.put("timestamp", rec.timestamp.format(formatter));
            historyList.add(hist);
        }

        // Prepare lists for return
        List<Map<String, Object>> byTask = new ArrayList<>();
        double finalTotalCost = totalCost;
        for (TaskAgg agg : taskAggs.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskName", agg.taskName);
            item.put("cost", agg.cost);
            item.put("tokens", agg.tokens);
            item.put("percent", finalTotalCost > 0 ? (agg.cost / finalTotalCost) * 100 : 0.0);
            byTask.add(item);
        }

        List<Map<String, Object>> byModel = new ArrayList<>();
        for (ModelAgg agg : modelAggs.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("modelName", agg.modelName);
            item.put("cost", agg.cost);
            item.put("tokens", agg.tokens);
            item.put("runs", agg.runs);
            item.put("avgCost", agg.runs > 0 ? agg.cost / agg.runs : 0.0);
            byModel.add(item);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalCost", totalCost);
        report.put("totalTokens", totalTokens);
        report.put("byTask", byTask);
        report.put("byModel", byModel);
        report.put("history", historyList);
        return report;
    }

    private static class CostRecord {
        String taskId;
        String taskName;
        String modelName;
        long promptTokens;
        long completionTokens;
        double cost;
        LocalDateTime timestamp;

        CostRecord(String taskId, String taskName, String modelName, long promptTokens, long completionTokens, double cost, LocalDateTime timestamp) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.modelName = modelName;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.cost = cost;
            this.timestamp = timestamp;
        }
    }

    private static class TaskAgg {
        String taskName;
        double cost = 0.0;
        long tokens = 0;

        TaskAgg(String taskName) {
            this.taskName = taskName;
        }

        void add(double c, long t) {
            this.cost += c;
            this.tokens += t;
        }
    }

    private static class ModelAgg {
        String modelName;
        double cost = 0.0;
        long tokens = 0;
        int runs = 0;

        ModelAgg(String modelName) {
            this.modelName = modelName;
        }

        void add(double c, long t) {
            this.cost += c;
            this.tokens += t;
            this.runs += 1;
        }
    }
}
