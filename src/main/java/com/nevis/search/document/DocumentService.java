package com.nevis.search.document;

import com.nevis.search.client.ClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientService clientService;
    private final DocumentIndexingService indexingService;

    public DocumentService(DocumentRepository documentRepository,
                           ClientService clientService,
                           DocumentIndexingService indexingService) {
        this.documentRepository = documentRepository;
        this.clientService = clientService;
        this.indexingService = indexingService;
    }

    @Transactional
    public Document create(UUID clientId, CreateDocumentRequest request) {
        var client = clientService.findById(clientId);
        var document = new Document(client, request.title(), request.content());
        document = documentRepository.save(document);
        indexingService.indexDocument(document);
        return document;
    }
}
