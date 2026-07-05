package com.farmmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TelemetryService telemetryService;

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "qwen2.5-coder:7b";

    private static final String KB_DIR = "C:\\Users\\VEDANTH\\farm-manager-ai\\farm_knowledge_base";
    private final File vaultDir = new File(KB_DIR, "vault");
    private final File pendingDir = new File(KB_DIR, "pending");
    private final File soulFile = new File(KB_DIR, "farm_soul.md");

    public KnowledgeBaseService(ObjectMapper objectMapper, TelemetryService telemetryService) {
        this.objectMapper = objectMapper;
        this.telemetryService = telemetryService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @PostConstruct
    public void init() {
        log.info("KnowledgeBaseService: Initializing Farm Knowledge Base directories...");
        try {
            if (!vaultDir.exists()) {
                Files.createDirectories(vaultDir.toPath());
                log.info("KnowledgeBaseService: Created vault directory: {}", vaultDir.getAbsolutePath());
            }
            if (!pendingDir.exists()) {
                Files.createDirectories(pendingDir.toPath());
                log.info("KnowledgeBaseService: Created pending directory: {}", pendingDir.getAbsolutePath());
            }

            seedDefaultNotes();
            seedDefaultSoul();
        } catch (IOException e) {
            log.error("KnowledgeBaseService: Failed to create directories", e);
        }
    }

    private void seedDefaultSoul() throws IOException {
        if (!soulFile.exists()) {
            log.info("KnowledgeBaseService: Seeding default farm_soul.md...");
            String defaultSoul = """
                    # Glorious Tiger Farms Profile
                    
                    **Owner/Operator**: Vedanth
                    **Location**: North Valley Agrisector
                    **Specializations**:
                    - Holstein Dairy Cattle
                    - Organic Heirloom Tomatoes (Rows 1-6)
                    - Free-Range Egg Production (Laying Hens)
                    
                    **Key Contacts**:
                    - Veterinarian: Dr. Sarah Jenkins (555-0199)
                    - Agronomist: Dr. Patel (555-0244)
                    - Feed Supplier: Valley Ag Supply (555-0102)
                    
                    **Active Farm Goals**:
                    - Maintain daily average milk yield above 300 gallons.
                    - Monitor tomato crops for early blight symptoms.
                    - Complete sheep deworming schedule before winter.
                    """;
            Files.writeString(soulFile.toPath(), defaultSoul, StandardCharsets.UTF_8);
            log.info("KnowledgeBaseService: Seeded farm_soul.md successfully.");
        }
    }

    public String getFarmSoul() {
        try {
            if (!soulFile.exists()) {
                seedDefaultSoul();
            }
            return Files.readString(soulFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read farm soul file: {}", soulFile.getAbsolutePath(), e);
            return "";
        }
    }

    public void saveFarmSoul(String content) {
        try {
            Files.writeString(soulFile.toPath(), content, StandardCharsets.UTF_8);
            log.info("Successfully updated farm_soul.md");
        } catch (IOException e) {
            log.error("Failed to write farm soul file: {}", soulFile.getAbsolutePath(), e);
        }
    }

    private void seedDefaultNotes() throws IOException {
        // Seed Approved Vault Notes if empty
        File[] approvedFiles = vaultDir.listFiles((dir, name) -> name.endsWith(".md"));
        if (approvedFiles == null || approvedFiles.length == 0) {
            log.info("KnowledgeBaseService: Seeding default approved notes in vault...");
            
            String cropRotationNote = """
                    # Crop Rotation and Soil Health Guidelines
                    
                    **Tags**: #soil-health #crops #tomatoes #legumes
                    **Date**: 2026-06-20
                    **Source**: AgriExtension Expert Article
                    
                    ## Overview
                    Rotating crops prevents soil depletion, breaks pest/disease cycles, and enhances soil organic matter. For family farms, a 3-year or 4-year rotation cycle is recommended.
                    
                    ## Recommended Rotation Sequence
                    1. **Nitrogen Fixers (Legumes)**: Peas, beans, alfalfa. They replenish nitrogen in the soil.
                    2. **Heavy Feeders (Leafy/Fruiting)**: Tomatoes, corn, lettuce, cabbage. They consume high amounts of nitrogen.
                    3. **Light Feeders (Root Crops)**: Carrots, onions, radishes. They require less nutrients.
                    
                    ## Key Rules
                    - Never plant Solanaceae (tomatoes, peppers, potatoes) in the same soil for two consecutive years.
                    - Always plant a cover crop (like winter rye or clover) in fall to prevent erosion and add biomass.
                    """;
            
            String vetProtocolNote = """
                    # Livestock Veterinary Protocol: Bovine Mastitis
                    
                    **Tags**: #livestock #dairy #cows #health #veterinary
                    **Date**: 2026-06-18
                    **Source**: Dr. Sarah Jenkins (Vet Clinic)
                    
                    ## Overview
                    Mastitis is the inflammation of the mammary gland in dairy cows, usually caused by bacterial infection. It reduces milk yield and quality.
                    
                    ## Symptoms
                    - **Clinical**: Swelling, heat, pain, or redness in the udder. Clumps, flakes, or watery milk.
                    - **Subclinical**: No visible symptoms, but elevated Somatic Cell Count (SCC).
                    
                    ## Treatment Steps
                    1. **Isolation**: Immediately move the affected cow to a hospital pen.
                    2. **Milking**: Strip out the infected quarter completely at least 3-4 times daily.
                    3. **Antibiotics**: Under vet guidance, administer intramammary infusions. Ensure a milk withdrawal period of 72-96 hours.
                    4. **Sanitation**: Clean and disinfect milking equipment before milking the next cow.
                    """;

            Files.writeString(new File(vaultDir, "crop_rotation_guide.md").toPath(), cropRotationNote, StandardCharsets.UTF_8);
            Files.writeString(new File(vaultDir, "livestock_vet_protocol.md").toPath(), vetProtocolNote, StandardCharsets.UTF_8);
            log.info("KnowledgeBaseService: Seeded vault notes successfully.");
        }

        // Seed Pending Notes if empty
        File[] pendingFiles = pendingDir.listFiles();
        if (pendingFiles == null || pendingFiles.length == 0) {
            log.info("KnowledgeBaseService: Seeding default pending notes...");
            
            String rawVetTranscript = """
                    Transcript of Call with Dr. Jenkins - June 23, 2026
                    Ummm, so yeah, we talked about the sheep. They need their, uh, deworming scheduled because of the wet weather. Wet pasture is bad for parasites. We should use Valbazen or ProMectin, but be careful with pregnant ewes - do not use Valbazen on ewes in their first 30 days of pregnancy! Very important.
                    Uhh, dosage is usually like 0.75 ml per 10 lbs body weight for sheep. Check the label though. Schedule it for next Tuesday morning. Tell the crew to get the sorting chutes ready.
                    Also she mentioned we should check their hooves for rot. The damp mud is causing hoof rot issues in the lower pasture. We need a copper sulfate footbath. Let's do that at the same time.
                    """;

            String rawCropDiseaseLog = """
                    Tomato Blight notes - June 24
                    Found some dark brown spots on the lower tomato leaves in row 4. Looks like early blight (Alternaria solani). The leaves are yellowing around the spots and falling off.
                    I think we need to spray them. Dr. Patel said to use copper fungicide. Spray every 7 to 10 days until dry weather returns.
                    Also need to prune the lower branches to improve airflow, keep the leaves dry. Make sure to disinfect the shears between plants so we don't spread it to row 5 or 6. Watering should be done at the soil level, not overhead. Overhead watering makes it worse.
                    """;

            Files.writeString(new File(pendingDir, "raw_vet_transcript_2026.txt").toPath(), rawVetTranscript, StandardCharsets.UTF_8);
            Files.writeString(new File(pendingDir, "crop_disease_notes.txt").toPath(), rawCropDiseaseLog, StandardCharsets.UTF_8);
            log.info("KnowledgeBaseService: Seeded pending notes successfully.");
        }
    }

    // --- Approved Vault Management ---

    public List<Map<String, String>> listApprovedNotes() {
        List<Map<String, String>> notes = new ArrayList<>();
        File[] files = vaultDir.listFiles((dir, name) -> name.endsWith(".md"));
        if (files != null) {
            for (File f : files) {
                Map<String, String> noteMap = new HashMap<>();
                noteMap.put("filename", f.getName());
                noteMap.put("title", f.getName().replace(".md", "").replace("_", " "));
                
                // Read tags/source from first few lines if available
                try {
                    List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                    String tags = "";
                    String source = "";
                    for (int i = 0; i < Math.min(10, lines.size()); i++) {
                        String line = lines.get(i);
                        if (line.toLowerCase().startsWith("**tags**:")) {
                            tags = line.substring(line.indexOf(":") + 1).trim();
                        } else if (line.toLowerCase().startsWith("**source**:")) {
                            source = line.substring(line.indexOf(":") + 1).trim();
                        }
                    }
                    noteMap.put("tags", tags);
                    noteMap.put("source", source);
                } catch (Exception e) {
                    noteMap.put("tags", "");
                    noteMap.put("source", "");
                }
                notes.add(noteMap);
            }
        }
        notes.sort((a, b) -> a.get("title").compareToIgnoreCase(b.get("title")));
        return notes;
    }

    public String getApprovedNoteContent(String filename) {
        File file = new File(vaultDir, filename);
        if (!file.exists()) {
            throw new IllegalArgumentException("Note " + filename + " does not exist.");
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read note content: {}", filename, e);
            return "Error reading file: " + e.getMessage();
        }
    }

    public void deleteApprovedNote(String filename) {
        File file = new File(vaultDir, filename);
        if (file.exists()) {
            file.delete();
            log.info("Deleted approved note: {}", filename);
        }
    }

    // --- Pending Notes Management ---

    public List<Map<String, String>> listPendingNotes() {
        List<Map<String, String>> notes = new ArrayList<>();
        File[] files = pendingDir.listFiles();
        if (files != null) {
            for (File f : files) {
                Map<String, String> noteMap = new HashMap<>();
                noteMap.put("filename", f.getName());
                try {
                    String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    noteMap.put("content", content);
                    // Extract preview (first 100 characters)
                    String preview = content.substring(0, Math.min(100, content.length())) + "...";
                    noteMap.put("preview", preview);
                } catch (IOException e) {
                    noteMap.put("content", "");
                    noteMap.put("preview", "Error reading content");
                }
                notes.add(noteMap);
            }
        }
        notes.sort((a, b) -> a.get("filename").compareToIgnoreCase(b.get("filename")));
        return notes;
    }

    public void deletePendingNote(String filename) {
        File file = new File(pendingDir, filename);
        if (file.exists()) {
            file.delete();
            log.info("Deleted pending note: {}", filename);
        }
    }

    // --- AI Operations ---

    /**
     * Use Ollama to clean up, structure, and format raw voice transcripts or notes.
     */
    public String lintPendingNote(String filename) throws Exception {
        File file = new File(pendingDir, filename);
        if (!file.exists()) {
            throw new IllegalArgumentException("Pending file does not exist: " + filename);
        }

        String rawContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        log.info("KnowledgeBaseService: Linting pending note '{}' with Ollama...", filename);

        String prompt = "You are an AI agricultural editor. Your job is to format and lint this raw farm log/voice transcription.\n\n" +
                "Rules:\n" +
                "- Clean up filler words, spelling mistakes, and conversational clutter.\n" +
                "- Structure it into clean markdown using clear headers (e.g. Overview, Symptoms, Recommended Action).\n" +
                "- Include a metadata header at the top matching exactly this layout:\n" +
                "  # [Polished Title]\n" +
                "  \n" +
                "  **Tags**: #[tag1] #[tag2]\n" +
                "  **Date**: [YYYY-MM-DD]\n" +
                "  **Source**: [Original Source (e.g. Phone Transcript, Pruning log)]\n" +
                "  \n" +
                "- Write in a professional, clear tone.\n" +
                "- Output ONLY the polished markdown. Do NOT write any chat introduction or explanation.\n\n" +
                "Raw Text:\n" +
                rawContent + "\n\n" +
                "Polished Markdown:";

        String polished = callOllama(prompt);
        log.info("KnowledgeBaseService: Successfully linted pending note '{}'.", filename);
        return polished;
    }

    /**
     * Save the polished content to the approved vault and delete the original pending file.
     */
    public void approveNote(String originalFilename, String title, String content) throws IOException {
        log.info("KnowledgeBaseService: Approving note '{}' as '{}.md'...", originalFilename, title);
        
        // Sanitize title for filename
        String sanitizedTitle = title.trim().toLowerCase().replaceAll("[^a-zA-Z0-9\\-_]", "_");
        if (sanitizedTitle.isEmpty()) {
            sanitizedTitle = "approved_note_" + System.currentTimeMillis();
        }
        String filename = sanitizedTitle + ".md";

        // Save to vault
        File targetFile = new File(vaultDir, filename);
        Files.writeString(targetFile.toPath(), content, StandardCharsets.UTF_8);
        log.info("KnowledgeBaseService: Saved note to vault: {}", targetFile.getAbsolutePath());

        // Delete original pending note
        if (originalFilename != null && !originalFilename.isEmpty()) {
            File pendingFile = new File(pendingDir, originalFilename);
            if (pendingFile.exists()) {
                pendingFile.delete();
                log.info("KnowledgeBaseService: Deleted original pending file: {}", pendingFile.getAbsolutePath());
            }
        }
    }

    /**
     * Query the knowledge base using Retrieval-Augmented Generation (RAG).
     */
    public Map<String, String> queryRAG(String query) throws Exception {
        log.info("KnowledgeBaseService: Running Wiki RAG search for query: '{}'", query);

        List<File> files = new ArrayList<>();
        File[] allFiles = vaultDir.listFiles((dir, name) -> name.endsWith(".md"));
        if (allFiles != null) {
            files.addAll(Arrays.asList(allFiles));
        }

        if (files.isEmpty()) {
            return Map.of(
                "answer", "The farm knowledge base is currently empty. Please add approved notes or guidelines to the vault first.",
                "sources", ""
            );
        }

        // Score documents based on keyword matching
        String[] queryKeywords = query.toLowerCase().split("\\s+");
        List<DocScore> docScores = new ArrayList<>();

        for (File f : files) {
            String content = Files.readString(f.toPath(), StandardCharsets.UTF_8).toLowerCase();
            int score = 0;
            for (String kw : queryKeywords) {
                if (kw.length() > 2 && content.contains(kw)) {
                    score += 10; // Major match
                    // Extra points for matches in title or tags
                    String nameLower = f.getName().toLowerCase();
                    if (nameLower.contains(kw)) {
                        score += 15;
                    }
                }
            }
            if (score > 0) {
                docScores.add(new DocScore(f, score));
            }
        }

        // Sort descending by score
        docScores.sort((a, b) -> Integer.compare(b.score, a.score));

        // Get top 2 documents
        List<DocScore> selectedDocs = docScores.stream().limit(2).collect(Collectors.toList());

        StringBuilder contextBuilder = new StringBuilder();
        List<String> citedSources = new ArrayList<>();

        if (selectedDocs.isEmpty()) {
            // Fallback: If no files match keywords, use the top 1 file in the vault to provide some general context or stay silent.
            log.info("KnowledgeBaseService: No matching documents found for RAG search.");
        } else {
            for (DocScore ds : selectedDocs) {
                String fileContent = Files.readString(ds.file.toPath(), StandardCharsets.UTF_8);
                contextBuilder.append("---\n")
                        .append("File: ").append(ds.file.getName()).append("\n")
                        .append(fileContent).append("\n");
                citedSources.add(ds.file.getName());
            }
        }

        String context = contextBuilder.toString();
        log.info("KnowledgeBaseService: Cited sources: {}", citedSources);

        String soulContent = getFarmSoul();
        String prompt;
        if (context.isEmpty()) {
            prompt = "You are a helpful AI farm operations assistant.\n" +
                    "Farm Profile:\n" + soulContent + "\n\n" +
                    "The user asked: \"" + query + "\"\n\n" +
                    "Currently, there are no specific notes in the local farm knowledge base that address this query.\n" +
                    "Explain that you couldn't find relevant documents in the local wiki, but answer the query using general agricultural best practices in a professional manner, keeping the Farm Profile context in mind.";
        } else {
            prompt = "You are a helpful AI farm operations assistant.\n" +
                    "Farm Profile:\n" + soulContent + "\n\n" +
                    "Below are reference notes from our local farm knowledge base:\n" +
                    context + "\n" +
                    "Using the provided reference notes and Farm Profile above, answer the following question. Be specific, clear, and professional. Mention the sources if they directly apply.\n\n" +
                    "Question: " + query + "\n" +
                    "Answer:";
        }

        String answer = callOllama(prompt);
        
        // Increment telemetry for AI usage
        telemetryService.incrementAiSuccess();

        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);
        response.put("sources", String.join(", ", citedSources));
        return response;
    }

    private static class DocScore {
        final File file;
        final int score;

        DocScore(File file, int score) {
            this.file = file;
            this.score = score;
        }
    }

    // --- Helper to execute Ollama requests ---

    private String callOllama(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP status code " + response.statusCode());
        }

        Map<?, ?> responseMap = objectMapper.readValue(response.body(), Map.class);
        return ((String) responseMap.get("response")).trim();
    }
}
