package com.nevis.search.client;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository);
    }

    @Test
    void create_savesClientWithCorrectFields() {
        var request = new CreateClientRequest("John", "Doe", "john@example.com",
                "Portfolio manager", List.of("https://linkedin.com/in/johndoe"));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        clientService.create(request);

        var captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        Client saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getDescription()).isEqualTo("Portfolio manager");
        assertThat(saved.getSocialLinks()).containsExactly("https://linkedin.com/in/johndoe");
    }

    @Test
    void create_withNullOptionalFields_savesSuccessfully() {
        var request = new CreateClientRequest("Jane", "Smith", "jane@example.com", null, null);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client result = clientService.create(request);

        assertThat(result.getDescription()).isNull();
        assertThat(result.getSocialLinks()).isNull();
    }

    @Test
    void findById_existingClient_returnsClient() {
        var id = UUID.randomUUID();
        var client = new Client("John", "Doe", "john@example.com", null, null);
        when(clientRepository.findById(id)).thenReturn(Optional.of(client));

        Client result = clientService.findById(id);

        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findById_nonExistingClient_throwsEntityNotFoundException() {
        var id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void search_delegatesToRepository() {
        var clients = List.of(new Client("John", "Doe", "john@example.com", null, null));
        when(clientRepository.searchByText("john", 20)).thenReturn(clients);

        List<Client> results = clientService.search("john", 20);

        assertThat(results).hasSize(1);
        verify(clientRepository).searchByText("john", 20);
    }

    @Test
    void search_emptyResults_returnsEmptyList() {
        when(clientRepository.searchByText("xyz", 20)).thenReturn(List.of());

        List<Client> results = clientService.search("xyz", 20);

        assertThat(results).isEmpty();
    }
}
