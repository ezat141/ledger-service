package com.ledger.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks")
@AutoConfigureMockMvc
class LedgerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void returnsEntriesForAnAuthenticatedCaller() throws Exception {
        mockMvc.perform(get("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("ezzat").claim("tenant", "acme"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].reference").value("LDG-1001"));
    }
}
