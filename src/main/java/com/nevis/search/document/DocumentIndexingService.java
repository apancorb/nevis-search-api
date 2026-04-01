package com.nevis.search.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final ChatClient chatClient;

    public DocumentIndexingService(VectorStore vectorStore,
                                   DocumentRepository documentRepository,
                                   @Nullable ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
        this.chatClient = chatClient;
    }

    @Async
    public void indexDocument(Document document) {
        embedDocument(document);
        summarizeDocument(document);
    }

    private void embedDocument(Document document) {
        try {
            String textToEmbed = document.getTitle() + "\n" + document.getContent();
            org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(textToEmbed, Map.of(
                    "document_id", document.getId().toString(),
                    "client_id", document.getClientId().toString(),
                    "title", document.getTitle()
            ));
            vectorStore.add(List.of(aiDoc));
            log.info("Indexed document {} in vector store", document.getId());
        } catch (Exception e) {
            log.error("Failed to embed document {}: {}", document.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    protected void summarizeDocument(Document document) {
        if (chatClient == null) {
            log.info("Skipping summarization for document {} (no ChatClient configured — set OPENAI_API_KEY to enable)",
                    document.getId());
            return;
        }

        try {
            String summary = chatClient.prompt()
                    .user(u -> u.text("""
                            Summarize this document in 1-2 sentences for a wealth management context.
                            Focus on what type of document it is and its key information.

                            Title: {title}
                            Content: {content}
                            """)
                            .param("title", document.getTitle())
                            .param("content", document.getContent()))
                    .call()
                    .content();

            document.setSummary(summary);
            documentRepository.save(document);
            log.info("Generated summary for document {}", document.getId());
        } catch (Exception e) {
            log.warn("Failed to generate summary for document {}: {}", document.getId(), e.getMessage());
        }
    }
}
