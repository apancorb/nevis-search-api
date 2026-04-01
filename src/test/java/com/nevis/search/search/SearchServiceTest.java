package com.nevis.search.search;

import com.nevis.search.client.Client;
import com.nevis.search.client.ClientService;
import com.nevis.search.document.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ClientService clientService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentRepository documentRepository;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(clientService, vectorStore, documentRepository, 20);
    }

    @Test
    void search_emptyResults_returnsEmptyResponse() {
        when(clientService.search(anyString(), anyInt())).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("nothing");

        assertThat(response.query()).isEqualTo("nothing");
        assertThat(response.results()).isEmpty();
        assertThat(response.meta().totalResults()).isZero();
    }

    @Test
    void search_clientResults_scoredByMatchType() {
        var emailMatch = new Client("John", "Doe", "john@neviswealth.com", null, null);
        var nameMatch = new Client("Nevis", "Test", "other@example.com", null, null);

        when(clientService.search(eq("nevis"), anyInt())).thenReturn(List.of(emailMatch, nameMatch));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("nevis");

        assertThat(response.results()).hasSize(2);
        var emailResult = response.results().stream()
                .filter(r -> r.client() != null && r.client().email().equals("john@neviswealth.com"))
                .findFirst().orElseThrow();
        var nameResult = response.results().stream()
                .filter(r -> r.client() != null && r.client().email().equals("other@example.com"))
                .findFirst().orElseThrow();

        assertThat(emailResult.score()).isGreaterThan(nameResult.score());
    }

    @Test
    void search_resultsAreSortedByScoreDescending() {
        var client1 = new Client("Test", "User", "test@neviswealth.com", "nevis description", null);
        var client2 = new Client("Nevis", "Admin", "admin@other.com", null, null);

        when(clientService.search(eq("nevis"), anyInt())).thenReturn(List.of(client1, client2));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("nevis");

        for (int i = 1; i < response.results().size(); i++) {
            assertThat(response.results().get(i - 1).score())
                    .isGreaterThanOrEqualTo(response.results().get(i).score());
        }
    }

    @Test
    void search_exactEmailMatch_scoresHighest() {
        var client = new Client("John", "Doe", "nevis", null, null);
        when(clientService.search(eq("nevis"), anyInt())).thenReturn(List.of(client));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("nevis");

        assertThat(response.results().get(0).score()).isEqualTo(1.0);
    }

    @Test
    void search_descriptionMatch_scoresLowerThanEmailMatch() {
        var emailClient = new Client("John", "Doe", "john@nevis.com", null, null);
        var descClient = new Client("Jane", "Smith", "jane@other.com", "Works at nevis", null);

        when(clientService.search(eq("nevis"), anyInt())).thenReturn(List.of(emailClient, descClient));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("nevis");

        var emailResult = response.results().stream()
                .filter(r -> r.client().email().equals("john@nevis.com"))
                .findFirst().orElseThrow();
        var descResult = response.results().stream()
                .filter(r -> r.client().email().equals("jane@other.com"))
                .findFirst().orElseThrow();

        assertThat(emailResult.score()).isGreaterThan(descResult.score());
    }

    @Test
    void search_metaContainsCorrectLimit() {
        when(clientService.search(anyString(), anyInt())).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("test");

        assertThat(response.meta().limit()).isEqualTo(20);
    }

    @Test
    void search_metaContainsSearchTime() {
        when(clientService.search(anyString(), anyInt())).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("test");

        assertThat(response.meta().searchTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void search_clientSearchFailure_returnsDocumentResultsOnly() {
        when(clientService.search(anyString(), anyInt())).thenThrow(new RuntimeException("DB down"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("test");

        assertThat(response.results()).isEmpty();
        assertThat(response.meta().totalResults()).isZero();
    }

    @Test
    void search_vectorSearchFailure_returnsClientResultsOnly() {
        var client = new Client("John", "Doe", "john@test.com", null, null);
        when(clientService.search(eq("test"), anyInt())).thenReturn(List.of(client));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("Vector store down"));

        SearchResponse response = searchService.search("test");

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).type()).isEqualTo("client");
    }

    @Test
    void search_respectsResultLimit() {
        var searchService5 = new SearchService(clientService, vectorStore, documentRepository, 5);

        var clients = List.of(
                new Client("A", "1", "a1@test.com", null, null),
                new Client("B", "2", "b2@test.com", null, null),
                new Client("C", "3", "c3@test.com", null, null),
                new Client("D", "4", "d4@test.com", null, null),
                new Client("E", "5", "e5@test.com", null, null),
                new Client("F", "6", "f6@test.com", null, null)
        );
        when(clientService.search(anyString(), anyInt())).thenReturn(clients);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService5.search("test");

        assertThat(response.results()).hasSize(5);
    }
}
