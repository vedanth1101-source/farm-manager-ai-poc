package com.farmmanager.controller;

import com.farmmanager.service.QueryTemplateService;
import com.farmmanager.dto.QueryRequest;
import com.farmmanager.service.QueryTemplateService.QueryResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows React frontend to connect easily
public class QueryController {

    private final QueryTemplateService queryTemplateService;

    public QueryController(QueryTemplateService queryTemplateService) {
        this.queryTemplateService = queryTemplateService;
    }

        @PostMapping("/query")
    public QueryResult executeQuery(@RequestBody QueryRequest request) {
            String question = request.getQuestion();
            return queryTemplateService.processQuestion(question);
        }

    @GetMapping("/questions")
    public List<String> getSupportedQuestions() {
        return queryTemplateService.getSupportedQuestions();
    }
}
