package com.nevis.search.document;

import com.nevis.search.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentRepository documentRepository;

    private DocumentIndexingService indexingService;

    @BeforeEach
    void setUp() {
        // null ChatClient = no summarization
        indexingService = new DocumentIndexingService(vectorStore, documentRepository, null);
    }

    @Test
    void indexDocument_addsToVectorStore() {
        var document = createDocument("Utility Bill", "Electricity bill for 123 Main St");

        indexingService.indexDocument(document);

        verify(vectorStore).add(any(List.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void indexDocument_embeddingContainsTitleAndContent() {
        var document = createDocument("Utility Bill", "Electricity bill content");

        indexingService.indexDocument(document);

        var captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<org.springframework.ai.document.Document> docs = captor.getValue();
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getText()).contains("Utility Bill");
        assertThat(docs.get(0).getText()).contains("Electricity bill content");
    }

    @Test
    @SuppressWarnings("unchecked")
    void indexDocument_metadataContainsDocumentAndClientId() {
        var document = createDocument("Title", "Content");

        indexingService.indexDocument(document);

        var captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<org.springframework.ai.document.Document> docs = captor.getValue();
        var metadata = docs.get(0).getMetadata();
        assertThat(metadata).containsKey("document_id");
        assertThat(metadata).containsKey("client_id");
        assertThat(metadata).containsKey("title");
        assertThat(metadata.get("title")).isEqualTo("Title");
    }

    @Test
    void indexDocument_withNoChatClient_skipsSummarization() {
        var document = createDocument("Title", "Content");

        indexingService.indexDocument(document);

        verify(documentRepository, never()).save(any());
    }

    @Test
    void indexDocument_embeddingFailure_doesNotThrow() {
        var document = createDocument("Title", "Content");
        doThrow(new RuntimeException("Embedding API down")).when(vectorStore).add(any(List.class));

        assertThatCode(() -> indexingService.indexDocument(document))
                .doesNotThrowAnyException();
    }

    private Document createDocument(String title, String content) {
        var client = new Client("John", "Doe", "john@example.com", null, null);
        setId(client, UUID.randomUUID());
        var document = new Document(client, title, content);
        setId(document, UUID.randomUUID());
        return document;
    }

    private void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
