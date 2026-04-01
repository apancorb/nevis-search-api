package com.nevis.search.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nevis.search.client.ClientResponse;
import com.nevis.search.document.DocumentResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResult(
        String type,
        double score,
        ClientResponse client,
        DocumentResponse document
) {
    public static SearchResult ofClient(ClientResponse client, double score) {
        return new SearchResult("client", score, client, null);
    }

    public static SearchResult ofDocument(DocumentResponse document, double score) {
        return new SearchResult("document", score, null, document);
    }
}
