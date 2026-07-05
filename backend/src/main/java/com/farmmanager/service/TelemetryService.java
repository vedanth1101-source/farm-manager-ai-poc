package com.farmmanager.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TelemetryService {
    private final AtomicInteger aiSuccessCount = new AtomicInteger(0);
    private final AtomicInteger templateFallbackCount = new AtomicInteger(0);
    private final AtomicInteger sqlFailureCount = new AtomicInteger(0);
    private final AtomicReference<Double> accumulatedCost = new AtomicReference<>(0.0);

    public void incrementAiSuccess() {
        aiSuccessCount.incrementAndGet();
        // Add a simulated cost per AI call ($0.00015)
        accumulatedCost.updateAndGet(current -> current + 0.00015);
    }

    public void incrementTemplateFallback() {
        templateFallbackCount.incrementAndGet();
    }

    public void incrementSqlFailure() {
        sqlFailureCount.incrementAndGet();
        // Add a smaller cost for failed calls since they still hit the model
        accumulatedCost.updateAndGet(current -> current + 0.00010);
    }

    public void resetMetrics() {
        aiSuccessCount.set(0);
        templateFallbackCount.set(0);
        sqlFailureCount.set(0);
        accumulatedCost.set(0.0);
    }

    public Map<String, Object> getMetricsExtended() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("aiSuccessCount", aiSuccessCount.get());
        metrics.put("templateFallbackCount", templateFallbackCount.get());
        metrics.put("sqlFailureCount", sqlFailureCount.get());
        metrics.put("accumulatedCost", String.format("%.5f", accumulatedCost.get()));
        return metrics;
    }

    public Map<String, Integer> getMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("aiSuccessCount", aiSuccessCount.get());
        metrics.put("templateFallbackCount", templateFallbackCount.get());
        metrics.put("sqlFailureCount", sqlFailureCount.get());
        return metrics;
    }
}
