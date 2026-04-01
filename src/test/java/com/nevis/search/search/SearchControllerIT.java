package com.nevis.search.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class SearchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void search_missingQuery_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_emptyQuery_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/search").param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_noResults_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/search").param("q", "nonexistenttermxyz123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("nonexistenttermxyz123"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.meta.limit").value(20));
    }
}
