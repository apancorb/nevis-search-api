package com.nevis.search.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class SearchIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String clientId;

    @BeforeEach
    void setUp() throws Exception {
        // Create a client
        String response = mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "John",
                                    "lastName": "Doe",
                                    "email": "john.doe.%d@neviswealth.com",
                                    "description": "Senior portfolio manager at Nevis Wealth"
                                }
                                """.formatted(System.nanoTime())))
                .andReturn().getResponse().getContentAsString();

        clientId = objectMapper.readTree(response).get("id").asText();

        // Create documents and wait for async indexing
        mockMvc.perform(post("/clients/{id}/documents", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Utility Bill - March 2026",
                            "content": "Electricity bill from ConEd for the period of March 2026. Service address: 123 Main Street, New York, NY 10001. Total amount due: $142.50."
                        }
                        """));

        mockMvc.perform(post("/clients/{id}/documents", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Tax Return 2025",
                            "content": "Federal income tax return for the fiscal year 2025. Adjusted gross income: $350,000. Total tax liability: $87,500. Filing status: Married filing jointly."
                        }
                        """));

        // Wait for async embedding to complete
        Thread.sleep(3000);
    }

    @Test
    void searchBySubstring_shouldMatchClientEmail() throws Exception {
        // Assignment requirement: "NevisWealth" should return client with email containing neviswealth
        mockMvc.perform(get("/search").param("q", "NevisWealth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isNotEmpty())
                .andExpect(jsonPath("$.results[?(@.type == 'client')]").isNotEmpty())
                .andExpect(jsonPath("$.results[?(@.type == 'client')].client.email",
                        hasItem(containsString("neviswealth"))));
    }

    @Test
    void searchByName_shouldMatchClient() throws Exception {
        mockMvc.perform(get("/search").param("q", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isNotEmpty())
                .andExpect(jsonPath("$.results[?(@.type == 'client')]").isNotEmpty());
    }

    @Test
    void searchByDescription_shouldMatchClient() throws Exception {
        mockMvc.perform(get("/search").param("q", "portfolio manager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isNotEmpty())
                .andExpect(jsonPath("$.results[?(@.type == 'client')]").isNotEmpty());
    }

    @Test
    void semanticSearch_addressProof_shouldFindUtilityBill() throws Exception {
        // Assignment requirement: "address proof" should return documents containing "utility bill"
        mockMvc.perform(get("/search").param("q", "address proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isNotEmpty())
                .andExpect(jsonPath("$.results[?(@.type == 'document')]").isNotEmpty())
                .andExpect(jsonPath("$.results[?(@.type == 'document')].document.title",
                        hasItem(containsString("Utility Bill"))));
    }

    @Test
    void semanticSearch_shouldRankRelevantDocumentsHigher() throws Exception {
        // "electricity" should rank the utility bill higher than the tax return
        mockMvc.perform(get("/search").param("q", "electricity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].type").value("document"))
                .andExpect(jsonPath("$.results[0].document.title", containsString("Utility Bill")));
    }

    @Test
    void search_responseShape_shouldHaveCorrectStructure() throws Exception {
        mockMvc.perform(get("/search").param("q", "Nevis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("Nevis"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.meta.totalResults").isNumber())
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.searchTimeMs").isNumber());
    }

    @Test
    void search_resultsSortedByScoreDescending() throws Exception {
        String response = mockMvc.perform(get("/search").param("q", "NevisWealth"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode results = objectMapper.readTree(response).get("results");
        for (int i = 1; i < results.size(); i++) {
            double prev = results.get(i - 1).get("score").asDouble();
            double curr = results.get(i).get("score").asDouble();
            assert prev >= curr : "Results should be sorted by score descending";
        }
    }
}
