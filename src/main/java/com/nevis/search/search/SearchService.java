package com.nevis.search.search;

import com.nevis.search.client.Client;
import com.nevis.search.client.ClientResponse;
import com.nevis.search.client.ClientService;
import com.nevis.search.document.Document;
import com.nevis.search.document.DocumentRepository;
import com.nevis.search.document.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ClientService clientService;
    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final int resultLimit;

    public SearchService(ClientService clientService,
                         VectorStore vectorStore,
                         DocumentRepository documentRepository,
                         @Value("${search.result-limit:20}") int resultLimit) {
        this.clientService = clientService;
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
        this.resultLimit = resultLimit;
    }

    public SearchResponse search(String query) {
        long startTime = System.currentTimeMillis();

        // Run client and document searches in parallel
        CompletableFuture<List<SearchResult>> clientFuture = CompletableFuture.supplyAsync(() -> searchClients(query));
        CompletableFuture<List<SearchResult>> documentFuture = CompletableFuture.supplyAsync(() -> searchDocuments(query));

        List<SearchResult> clientResults = clientFuture.join();
        List<SearchResult> documentResults = documentFuture.join();

        // Merge, sort by score descending, limit
        List<SearchResult> allResults = Stream.concat(clientResults.stream(), documentResults.stream())
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(resultLimit)
                .toList();

        long elapsed = System.currentTimeMillis() - startTime;

        return new SearchResponse(
                query,
                allResults,
                new SearchResponse.SearchMeta(allResults.size(), resultLimit, elapsed)
        );
    }

    private List<SearchResult> searchClients(String query) {
        try {
            List<Client> clients = clientService.search(query, resultLimit);
            return clients.stream()
                    .map(client -> {
                        double score = scoreClientMatch(client.getEmail(), client.getFirstName(),
                                client.getLastName(), client.getDescription(), query);
                        return SearchResult.ofClient(ClientResponse.from(client), score);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Client search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<SearchResult> searchDocuments(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(resultLimit)
                    .build();

            List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(searchRequest);
            if (results == null || results.isEmpty()) {
                return List.of();
            }

            // Collect all document IDs and scores from vector results
            Map<UUID, Double> scoresByDocumentId = new LinkedHashMap<>();
            for (org.springframework.ai.document.Document doc : results) {
                Map<String, Object> metadata = doc.getMetadata();
                UUID documentId = UUID.fromString((String) metadata.get("document_id"));
                double score = doc.getScore() != null ? doc.getScore() : 0.0;
                scoresByDocumentId.put(documentId, score);
            }

            // Single batch query instead of N+1
            List<Document> documents = documentRepository.findAllById(scoresByDocumentId.keySet());
            Map<UUID, Document> documentsById = new HashMap<>();
            for (Document doc : documents) {
                documentsById.put(doc.getId(), doc);
            }

            return scoresByDocumentId.entrySet().stream()
                    .map(entry -> {
                        Document document = documentsById.get(entry.getKey());
                        if (document == null) return null;
                        return SearchResult.ofDocument(DocumentResponse.from(document), entry.getValue());
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Document search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private double scoreClientMatch(String email, String firstName, String lastName,
                                     String description, String query) {
        String lowerQuery = query.toLowerCase();

        // Exact email match
        if (email != null && email.equalsIgnoreCase(query)) return 1.0;
        // Email contains query
        if (email != null && email.toLowerCase().contains(lowerQuery)) return 0.9;
        // Name match
        String fullName = (firstName + " " + lastName).toLowerCase();
        if (fullName.contains(lowerQuery)) return 0.8;
        // Description match
        if (description != null && description.toLowerCase().contains(lowerQuery)) return 0.7;

        return 0.6;
    }
}
