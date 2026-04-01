package com.nevis.search.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/clients/{clientId}/documents")
@Tag(name = "Documents", description = "Client document management")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a document for a client")
    public DocumentResponse create(@PathVariable UUID clientId,
                                   @Valid @RequestBody CreateDocumentRequest request) {
        var document = documentService.create(clientId, request);
        return DocumentResponse.from(document);
    }
}
