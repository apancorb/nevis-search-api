package com.nevis.search.search;

import com.nevis.search.client.ClientResponse;
import com.nevis.search.client.ClientService;
import com.nevis.search.document.DocumentRepository;
import com.nevis.search.document.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
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
        var clientFuture = CompletableFuture.supplyAsync(() -> searchClients(query));
        var documentFuture = CompletableFuture.supplyAsync(() -> searchDocuments(query));

        var clientResults = clientFuture.join();
        var documentResults = documentFuture.join();

        // Merge, sort by score descending, limit
        var allResults = Stream.concat(clientResults.stream(), documentResults.stream())
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
            var clients = clientService.search(query, resultLimit);
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
            var searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(resultLimit)
                    .build();

            var results = vectorStore.similaritySearch(searchRequest);
            if (results == null || results.isEmpty()) {
                return List.of();
            }

            return results.stream()
                    .map(doc -> {
                        var metadata = doc.getMetadata();
                        var documentId = UUID.fromString((String) metadata.get("document_id"));
                        var document = documentRepository.findById(documentId).orElse(null);
                        if (document == null) return null;

                        double score = doc.getScore() != null ? doc.getScore() : 0.0;
                        return SearchResult.ofDocument(DocumentResponse.from(document), score);
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
