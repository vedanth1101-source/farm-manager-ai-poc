package com.farmmanager.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TelemetryService {
    private final AtomicInteger aiSuccessCount = new AtomicInteger(0);
    private final AtomicInteger templateFallbackCount = new AtomicInteger(0);
    private final AtomicInteger sqlFailureCount = new AtomicInteger(0);

    public void incrementAiSuccess() {
        aiSuccessCount.incrementAndGet();
    }

    public void incrementTemplateFallback() {
        templateFallbackCount.incrementAndGet();
    }

    public void incrementSqlFailure() {
        sqlFailureCount.incrementAndGet();
    }

    public Map<String, Integer> getMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("aiSuccessCount", aiSuccessCount.get());
        metrics.put("templateFallbackCount", templateFallbackCount.get());
        metrics.put("sqlFailureCount", sqlFailureCount.get());
        return metrics;
    }
}
