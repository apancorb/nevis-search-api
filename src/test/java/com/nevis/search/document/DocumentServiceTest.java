package com.nevis.search.document;

import com.nevis.search.client.Client;
import com.nevis.search.client.ClientService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private DocumentIndexingService indexingService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, clientService, indexingService);
    }

    @Test
    void create_savesDocumentWithCorrectFields() {
        var clientId = UUID.randomUUID();
        var client = new Client("John", "Doe", "john@example.com", null, null);
        var request = new CreateDocumentRequest("Utility Bill", "Electricity bill content");

        when(clientService.findById(clientId)).thenReturn(client);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        documentService.create(clientId, request);

        var captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Utility Bill");
        assertThat(saved.getContent()).isEqualTo("Electricity bill content");
        assertThat(saved.getClient()).isEqualTo(client);
    }

    @Test
    void create_triggersAsyncIndexing() {
        var clientId = UUID.randomUUID();
        var client = new Client("John", "Doe", "john@example.com", null, null);
        var request = new CreateDocumentRequest("Tax Return", "Tax content");

        when(clientService.findById(clientId)).thenReturn(client);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        documentService.create(clientId, request);

        verify(indexingService).indexDocument(any(Document.class));
    }

    @Test
    void create_clientNotFound_throwsEntityNotFoundException() {
        var clientId = UUID.randomUUID();
        var request = new CreateDocumentRequest("Title", "Content");

        when(clientService.findById(clientId)).thenThrow(new EntityNotFoundException("Client not found: " + clientId));

        assertThatThrownBy(() -> documentService.create(clientId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(clientId.toString());

        verify(documentRepository, never()).save(any());
        verify(indexingService, never()).indexDocument(any());
    }

    @Test
    void create_summaryIsNullInitially() {
        var clientId = UUID.randomUUID();
        var client = new Client("John", "Doe", "john@example.com", null, null);
        var request = new CreateDocumentRequest("Title", "Content");

        when(clientService.findById(clientId)).thenReturn(client);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.create(clientId, request);

        assertThat(result.getSummary()).isNull();
    }
}
