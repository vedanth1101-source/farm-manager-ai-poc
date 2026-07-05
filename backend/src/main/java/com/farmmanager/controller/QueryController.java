package com.farmmanager.controller;

import com.farmmanager.dto.QueryRequest;
import com.farmmanager.dto.QueryResponse;
import com.farmmanager.dto.GoalDTO;
import com.farmmanager.service.OllamaQueryService;
import com.farmmanager.service.OllamaQueryService.OllamaResult;
import com.farmmanager.service.QueryTemplateService;
import com.farmmanager.service.QueryTemplateService.QueryResult;
import com.farmmanager.service.TelemetryService;
import com.farmmanager.service.GoalsService;
import com.farmmanager.service.DailyBriefingService;
import com.farmmanager.service.GraphifyService;
import com.farmmanager.service.KnowledgeBaseService;
import com.farmmanager.service.BackgroundAgentService;
import com.farmmanager.service.ModelRegistryService;
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
    private final GoalsService goalsService;
    private final DailyBriefingService dailyBriefingService;
    private final GraphifyService graphifyService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final BackgroundAgentService backgroundAgentService;
    private final ModelRegistryService modelRegistryService;

    public QueryController(OllamaQueryService ollamaQueryService, 
                           QueryTemplateService queryTemplateService, 
                           TelemetryService telemetryService,
                           GoalsService goalsService,
                           DailyBriefingService dailyBriefingService,
                           GraphifyService graphifyService,
                           KnowledgeBaseService knowledgeBaseService,
                           BackgroundAgentService backgroundAgentService,
                           ModelRegistryService modelRegistryService) {
        this.ollamaQueryService = ollamaQueryService;
        this.queryTemplateService = queryTemplateService;
        this.telemetryService = telemetryService;
        this.goalsService = goalsService;
        this.dailyBriefingService = dailyBriefingService;
        this.graphifyService = graphifyService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.backgroundAgentService = backgroundAgentService;
        this.modelRegistryService = modelRegistryService;
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
    public Map<String, Object> getTelemetry() {
        log.info("Fetching telemetry metrics...");
        return telemetryService.getMetricsExtended();
    }

    @PostMapping("/telemetry/reset")
    public Map<String, String> resetTelemetry() {
        log.info("Resetting telemetry metrics...");
        telemetryService.resetMetrics();
        return Map.of("status", "success", "message", "Telemetry reset successfully");
    }

    @GetMapping("/goals")
    public List<GoalDTO> getGoals() {
        log.info("Fetching all farm goals...");
        return goalsService.getEvaluatedGoals();
    }

    @PostMapping("/goals")
    public Map<String, String> addGoal(@RequestBody GoalDTO goal) {
        log.info("Adding new farm goal: '{}'", goal.getName());
        goalsService.addGoal(goal);
        return Map.of("status", "success", "message", "Goal added successfully");
    }

    @GetMapping("/briefing")
    public Map<String, String> getBriefing() {
        log.info("Fetching daily farm briefing...");
        String briefing = dailyBriefingService.getDailyBriefing();
        return Map.of("briefing", briefing);
    }

    @PostMapping("/briefing/regenerate")
    public Map<String, String> regenerateBriefing() {
        log.info("Force regenerating daily farm briefing...");
        dailyBriefingService.forceRegenerate();
        String briefing = dailyBriefingService.getDailyBriefing();
        return Map.of("briefing", briefing);
    }

    // --- CODE GRAPH EXPLORER ENDPOINTS ---

    @GetMapping("/graph/nodes")
    public List<String> getGraphNodes() {
        log.info("Fetching codebase graph nodes...");
        return graphifyService.getCodebaseNodes();
    }

    @GetMapping("/graph/query")
    public Map<String, String> getGraphQuery(@RequestParam("q") String query) {
        log.info("Executing Graphify query for '{}'", query);
        String result = graphifyService.queryGraph(query);
        return Map.of("result", result);
    }

    @GetMapping("/graph/affected")
    public Map<String, String> getGraphAffected(@RequestParam("node") String node) {
        log.info("Executing Graphify affected check for '{}'", node);
        String result = graphifyService.getAffectedNodes(node);
        return Map.of("result", result);
    }

    // --- KNOWLEDGE BASE ENDPOINTS ---

    @GetMapping("/kb/notes")
    public List<Map<String, String>> getApprovedNotes() {
        log.info("Fetching approved knowledge base notes...");
        return knowledgeBaseService.listApprovedNotes();
    }

    @GetMapping("/kb/notes/{filename}")
    public Map<String, String> getApprovedNoteContent(@PathVariable String filename) {
        log.info("Fetching approved note content for: {}", filename);
        String content = knowledgeBaseService.getApprovedNoteContent(filename);
        return Map.of("content", content);
    }

    @DeleteMapping("/kb/notes/{filename}")
    public Map<String, String> deleteApprovedNote(@PathVariable String filename) {
        log.info("Deleting approved note: {}", filename);
        knowledgeBaseService.deleteApprovedNote(filename);
        return Map.of("status", "success");
    }

    @GetMapping("/kb/pending")
    public List<Map<String, String>> getPendingNotes() {
        log.info("Fetching pending ingestion notes...");
        return knowledgeBaseService.listPendingNotes();
    }

    @DeleteMapping("/kb/pending/{filename}")
    public Map<String, String> deletePendingNote(@PathVariable String filename) {
        log.info("Deleting pending note: {}", filename);
        knowledgeBaseService.deletePendingNote(filename);
        return Map.of("status", "success");
    }

    @PostMapping("/kb/lint")
    public Map<String, String> lintPendingNote(@RequestBody Map<String, String> payload) throws Exception {
        String filename = payload.get("filename");
        log.info("AI linting pending note: {}", filename);
        String content = knowledgeBaseService.lintPendingNote(filename);
        return Map.of("content", content);
    }

    @PostMapping("/kb/approve")
    public Map<String, String> approvePendingNote(@RequestBody Map<String, String> payload) throws Exception {
        String filename = payload.get("filename");
        String title = payload.get("title");
        String content = payload.get("content");
        log.info("Approving pending note: {} with title: {}", filename, title);
        knowledgeBaseService.approveNote(filename, title, content);
        return Map.of("status", "success");
    }

    @PostMapping("/kb/query")
    public Map<String, String> queryKnowledgeBase(@RequestBody Map<String, String> payload) throws Exception {
        String query = payload.get("query");
        log.info("Executing RAG query on knowledge base: '{}'", query);
        return knowledgeBaseService.queryRAG(query);
    }

    // --- AGENTIC OS ENDPOINTS ---

    @GetMapping("/kb/soul")
    public Map<String, String> getFarmSoul() {
        log.info("Fetching farm soul profile...");
        String content = knowledgeBaseService.getFarmSoul();
        return Map.of("content", content);
    }

    @PostMapping("/kb/soul")
    public Map<String, String> saveFarmSoul(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        log.info("Updating farm soul profile...");
        knowledgeBaseService.saveFarmSoul(content);
        return Map.of("status", "success");
    }

    @GetMapping("/agent/tasks")
    public List<Map<String, Object>> getAgentTasks() {
        log.info("Fetching background agent tasks list...");
        return backgroundAgentService.getTasks();
    }

    @PostMapping("/agent/run")
    public Map<String, String> runAgentTask(@RequestBody Map<String, String> payload) {
        String taskName = payload.get("task");
        log.info("Triggering background agent task: {} with payload: {}", taskName, payload);
        String taskId = backgroundAgentService.runTask(taskName, payload);
        return Map.of("status", "success", "taskId", taskId);
    }

    // --- MODEL INTEL ENDPOINTS ---

    @GetMapping("/models")
    public List<String> getAvailableModels() {
        log.info("Fetching available Ollama models...");
        return modelRegistryService.getAvailableModels();
    }

    @GetMapping("/models/routing")
    public Map<String, String> getModelRouting() {
        log.info("Fetching model task routing map...");
        return modelRegistryService.getRoutingMap();
    }

    @PostMapping("/models/routing")
    public Map<String, String> updateModelRouting(@RequestBody Map<String, String> payload) {
        String task = payload.get("task");
        String model = payload.get("model");
        log.info("Updating model routing for task: {} -> {}", task, model);
        modelRegistryService.updateRouting(task, model);
        return Map.of("status", "success");
    }

    @GetMapping("/models/costs")
    public Map<String, Object> getModelCosts() {
        log.info("Fetching model cost and usage intelligence report...");
        return modelRegistryService.getCostReport();
    }
}
