package com.nevis.search.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueEmail() {
        return "test-%s@neviswealth.com".formatted(UUID.randomUUID().toString().substring(0, 8));
    }

    @Test
    void createClient_returnsCreated() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "John",
                                    "lastName": "Doe",
                                    "email": "%s",
                                    "description": "Portfolio manager",
                                    "socialLinks": ["https://linkedin.com/in/johndoe"]
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.description").value("Portfolio manager"))
                .andExpect(jsonPath("$.socialLinks[0]").value("https://linkedin.com/in/johndoe"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createClient_missingRequiredFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "John"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createClient_invalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "John",
                                    "lastName": "Doe",
                                    "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_duplicateEmail_returnsConflict() throws Exception {
        String email = uniqueEmail();
        String body = """
                {
                    "firstName": "Jane",
                    "lastName": "Dup",
                    "email": "%s"
                }
                """.formatted(email);

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}
