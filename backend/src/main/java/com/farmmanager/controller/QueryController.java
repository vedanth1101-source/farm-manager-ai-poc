package com.farmmanager.controller;

import com.farmmanager.dto.QueryRequest;
import com.farmmanager.dto.QueryResponse;
import com.farmmanager.service.OllamaQueryService;
import com.farmmanager.service.OllamaQueryService.OllamaResult;
import com.farmmanager.service.QueryTemplateService;
import com.farmmanager.service.QueryTemplateService.QueryResult;
import com.farmmanager.service.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows React frontend to connect easily
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final OllamaQueryService ollamaQueryService;
    private final QueryTemplateService queryTemplateService;
    private final TelemetryService telemetryService;

    public QueryController(OllamaQueryService ollamaQueryService, 
                           QueryTemplateService queryTemplateService, 
                           TelemetryService telemetryService) {
        this.ollamaQueryService = ollamaQueryService;
        this.queryTemplateService = queryTemplateService;
        this.telemetryService = telemetryService;
    }

    @PostMapping("/query")
    public QueryResponse executeQuery(@RequestBody QueryRequest request) {
        String question = request.getQuestion();
        log.info("Received query request for question: '{}'", question);

        // Try AI execution first
        try {
            OllamaResult aiResult = ollamaQueryService.processQuestion(question);
            telemetryService.incrementAiSuccess();
            log.info("Query processed successfully in AI mode.");
            return new QueryResponse(
                    question,
                    aiResult.getSql(),
                    aiResult.getAnswer(),
                    aiResult.getColumns(),
                    aiResult.getRows(),
                    "ai"
            );
        } catch (BadSqlGrammarException | java.sql.SQLException e) {
            log.error("AI SQL Execution error: {}. Incrementing SQL failure count and falling back...", e.getMessage());
            telemetryService.incrementSqlFailure();
            return triggerFallback(question);
        } catch (Exception e) {
            log.warn("AI generation failed or service unavailable: {}. Falling back to template mode...", e.getMessage());
            return triggerFallback(question);
        }
    }

    private QueryResponse triggerFallback(String question) {
        log.info("Triggering template fallback for question: '{}'", question);
        telemetryService.incrementTemplateFallback();
        QueryResult templateResult = queryTemplateService.processQuestion(question);
        return new QueryResponse(
                templateResult.getQuestion(),
                templateResult.getSql(),
                templateResult.getAnswer(),
                templateResult.getColumns(),
                templateResult.getRows(),
                "template"
        );
    }

    @GetMapping("/questions")
    public List<String> getSupportedQuestions() {
        return queryTemplateService.getSupportedQuestions();
    }

    @GetMapping("/telemetry")
    public Map<String, Integer> getTelemetry() {
        log.info("Fetching telemetry metrics...");
        return telemetryService.getMetrics();
    }
}
