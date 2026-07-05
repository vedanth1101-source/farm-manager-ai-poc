package com.farmmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class GraphifyService {

    private static final Logger log = LoggerFactory.getLogger(GraphifyService.class);
    private final ObjectMapper objectMapper;

    private static final String PYTHON_PATH = "C:\\Users\\VEDANTH\\AppData\\Local\\Programs\\Python\\Python311\\python.exe";
    private static final String PROJECT_ROOT = "C:\\Users\\VEDANTH\\farm-manager-ai";
    private static final String MANIFEST_PATH = PROJECT_ROOT + "\\graphify-out\\manifest.json";

    public GraphifyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse manifest.json keys to retrieve codebase files/symbols.
     */
    public List<String> getCodebaseNodes() {
        log.info("GraphifyService: Parsing manifest.json for project files...");
        List<String> projectFiles = new ArrayList<>();
        try {
            File manifestFile = new File(MANIFEST_PATH);
            if (!manifestFile.exists()) {
                log.warn("manifest.json not found at: {}", MANIFEST_PATH);
                return projectFiles;
            }

            JsonNode rootNode = objectMapper.readTree(manifestFile);
            Iterator<String> fieldNames = rootNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fileName = fieldNames.next();
                // Filter out hidden folders, scripts, and internal agent directories
                if (!fileName.startsWith(".") && !fileName.startsWith("graphify-out/") && !fileName.startsWith("screenshots/")) {
                    projectFiles.add(fileName);
                }
            }
            projectFiles.sort(String::compareTo);
            log.info("GraphifyService: Loaded {} project files from manifest.", projectFiles.size());
        } catch (Exception e) {
            log.error("GraphifyService: Failed to parse manifest.json: {}", e.getMessage());
        }
        return projectFiles;
    }

    /**
     * Run python -m graphify query "<question>" using ProcessBuilder.
     */
    public String queryGraph(String question) {
        log.info("GraphifyService: Querying graph with question: '{}'", question);
        // Sanitize question input to prevent command injection
        String sanitized = question.replace("\"", "\\\"");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH,
                    "-m",
                    "graphify",
                    "query",
                    sanitized,
                    "--graph",
                    "graphify-out/graph.json"
            );
            pb.directory(new File(PROJECT_ROOT));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            log.info("Graphify query exited with code {}", exitCode);
            return output.toString().trim();
        } catch (Exception e) {
            log.error("GraphifyService: Failed to execute query process: {}", e.getMessage());
            return "Error executing Graphify query: " + e.getMessage();
        }
    }

    /**
     * Run python -m graphify affected "<node>" using ProcessBuilder.
     */
    public String getAffectedNodes(String node) {
        log.info("GraphifyService: Inspecting affected nodes for: '{}'", node);
        // Sanitize node string
        String sanitized = node.replace("\"", "\\\"");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH,
                    "-m",
                    "graphify",
                    "affected",
                    sanitized,
                    "--graph",
                    "graphify-out/graph.json"
            );
            pb.directory(new File(PROJECT_ROOT));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            log.info("Graphify affected exited with code {}", exitCode);
            return output.toString().trim();
        } catch (Exception e) {
            log.error("GraphifyService: Failed to execute affected process: {}", e.getMessage());
            return "Error executing Graphify affected check: " + e.getMessage();
        }
    }
}
