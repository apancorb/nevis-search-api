package com.nevis.search.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "content is required")
        @Size(max = 10000, message = "content must not exceed 10000 characters")
        String content
) {}
