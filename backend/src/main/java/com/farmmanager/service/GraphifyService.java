package com.farmmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class GraphifyService {

    private static final Logger log = LoggerFactory.getLogger(GraphifyService.class);
    private final ObjectMapper objectMapper;

    /**
     * Interpreter used to run the graphify CLI. Defaults to whatever `python`
     * resolves to on PATH; override with graphify.python-path when the
     * interpreter lives outside PATH.
     *
     * Must remain a real executable — pointing this at a .bat or .cmd would
     * make Windows route the call through cmd.exe, at which point the argument
     * array below would no longer be injection-safe.
     */
    private final String pythonPath;

    /** Project root the graphify CLI runs against. Defaults to the working directory. */
    private final Path projectRoot;

    public GraphifyService(
            ObjectMapper objectMapper,
            @Value("${graphify.python-path:python}") String pythonPath,
            @Value("${graphify.project-root:}") String projectRoot) {
        this.objectMapper = objectMapper;
        this.pythonPath = pythonPath;
        this.projectRoot = (projectRoot == null || projectRoot.isBlank())
                ? Paths.get("").toAbsolutePath()
                : Paths.get(projectRoot).toAbsolutePath();
    }

    private String manifestPath() {
        return projectRoot.resolve("graphify-out").resolve("manifest.json").toString();
    }

    /**
     * Parse manifest.json keys to retrieve codebase files/symbols.
     */
    public List<String> getCodebaseNodes() {
        log.info("GraphifyService: Parsing manifest.json for project files...");
        List<String> projectFiles = new ArrayList<>();
        try {
            File manifestFile = new File(manifestPath());
            if (!manifestFile.exists()) {
                log.warn("manifest.json not found at: {}", manifestPath());
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
        // No shell escaping needed: ProcessBuilder receives an argument array,
        // which is passed to the OS directly without shell interpretation.
        // Escaping quotes here would corrupt legitimate input, not protect it.

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    "-m",
                    "graphify",
                    "query",
                    question,
                    "--graph",
                    "graphify-out/graph.json"
            );
            pb.directory(projectRoot.toFile());
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
        // See queryGraph(): the argument array is not shell-interpreted.

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    "-m",
                    "graphify",
                    "affected",
                    node,
                    "--graph",
                    "graphify-out/graph.json"
            );
            pb.directory(projectRoot.toFile());
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
