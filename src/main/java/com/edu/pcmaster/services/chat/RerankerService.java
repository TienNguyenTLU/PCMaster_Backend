package com.edu.pcmaster.services.chat;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service quản lý Python Reranker Server và thực hiện rerank documents.
 * Sử dụng model BAAI/bge-reranker-large chạy trên Python Flask server.
 */
@Service
public class RerankerService {

    private static final int MAX_RERANK_DOCS = 15;

    @PostConstruct
    public void init() {
        startRerankerServer();
    }

    /**
     * Auto-start Python Reranker Server nếu chưa chạy.
     */
    private void startRerankerServer() {
        try (java.net.Socket socket = new java.net.Socket("127.0.0.1", 8090)) {
            System.out.println("[RAG Rerank] Reranker server is already running on port 8090.");
            return;
        } catch (IOException e) {
            System.out.println("[RAG Rerank] Port 8090 is free. Starting Python Reranker Server...");
        }

        new Thread(() -> {
            try {
                String venvPython = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "venv\\Scripts\\python.exe"
                        : "venv/bin/python";

                String projectPath = System.getProperty("user.dir");
                File pythonExe = new File(projectPath, venvPython);
                File scriptFile = new File(projectPath, "scripts\\reranker_server.py");

                if (!pythonExe.exists()) {
                    System.err.println("[RAG Rerank] Virtual environment python not found at: " + pythonExe.getAbsolutePath());
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(
                        pythonExe.getAbsolutePath(),
                        scriptFile.getAbsolutePath()
                );
                pb.directory(new File(projectPath));
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(projectPath, "reranker_server.log")));

                Process process = pb.start();
                System.out.println("[RAG Rerank] Python Reranker Server started. Process: " + process.pid());
            } catch (Exception ex) {
                System.err.println("[RAG Rerank] Failed to start Python Reranker Server: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Rerank documents dựa trên query sử dụng cross-encoder model.
     * Fallback về thứ tự gốc nếu reranker server không khả dụng.
     *
     * @param query câu hỏi gốc
     * @param docs  danh sách documents từ vector search
     * @return danh sách documents đã được sắp xếp lại theo relevance
     */
    public List<Document> rerankDocuments(String query, List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return docs;
        }

        List<Document> candidateDocs = docs.subList(0, Math.min(MAX_RERANK_DOCS, docs.size()));

        try {
            List<String> docTexts = candidateDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "documents", docTexts
            );

            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            List<Double> scores = restTemplate.postForObject(
                    "http://127.0.0.1:8090/rerank",
                    requestBody,
                    List.class
            );

            if (scores != null && scores.size() == candidateDocs.size()) {
                List<Map.Entry<Document, Double>> scoredDocs = new ArrayList<>();
                for (int i = 0; i < candidateDocs.size(); i++) {
                    scoredDocs.add(Map.entry(candidateDocs.get(i), scores.get(i)));
                }

                // Sort descending by relevance score
                scoredDocs.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                List<Document> reranked = scoredDocs.stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                // Append remaining documents not included in reranking
                if (docs.size() > MAX_RERANK_DOCS) {
                    reranked.addAll(docs.subList(MAX_RERANK_DOCS, docs.size()));
                }
                return reranked;
            }
        } catch (Exception e) {
            System.err.println("[RAG Rerank] Reranking failed, falling back to original order: " + e.getMessage());
        }

        return docs;
    }
}
