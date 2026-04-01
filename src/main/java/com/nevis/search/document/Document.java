package com.nevis.search.document;

import com.nevis.search.client.Client;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String summary;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Document() {}

    public Document(Client client, String title, String content) {
        this.client = client;
        this.title = title;
        this.content = content;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public UUID getId() { return id; }
    public Client getClient() { return client; }
    public UUID getClientId() { return client.getId(); }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
}
