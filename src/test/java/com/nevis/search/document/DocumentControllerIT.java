package com.nevis.search.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createClientAndGetId() throws Exception {
        String response = mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Doc",
                                    "lastName": "Test",
                                    "email": "doc.test.%s@example.com"
                                }
                                """.formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asText();
    }

    @Test
    void createDocument_returnsCreated() throws Exception {
        String clientId = createClientAndGetId();

        mockMvc.perform(post("/clients/{id}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Utility Bill",
                                    "content": "Electricity bill for 123 Main St, March 2026."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.title").value("Utility Bill"))
                .andExpect(jsonPath("$.content").value("Electricity bill for 123 Main St, March 2026."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createDocument_clientNotFound_returnsNotFound() throws Exception {
        String fakeId = UUID.randomUUID().toString();

        mockMvc.perform(post("/clients/{id}/documents", fakeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Some Doc",
                                    "content": "Some content"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDocument_missingTitle_returnsBadRequest() throws Exception {
        String clientId = createClientAndGetId();

        mockMvc.perform(post("/clients/{id}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "Some content"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocument_missingContent_returnsBadRequest() throws Exception {
        String clientId = createClientAndGetId();

        mockMvc.perform(post("/clients/{id}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Some Title"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
