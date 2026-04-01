package com.nevis.search.client;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client create(CreateClientRequest request) {
        Client client = new Client(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.description(),
                request.socialLinks()
        );
        return clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public Client findById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Client> search(String query, int limit) {
        return clientRepository.searchByText(query, limit);
    }
}
