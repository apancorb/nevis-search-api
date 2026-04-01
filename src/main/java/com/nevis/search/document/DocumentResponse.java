package com.nevis.search.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID clientId,
        String title,
        String content,
        String summary,
        Instant createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getClientId(),
                document.getTitle(),
                document.getContent(),
                document.getSummary(),
                document.getCreatedAt()
        );
    }
}
