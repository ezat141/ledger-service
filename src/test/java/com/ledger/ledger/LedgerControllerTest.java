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
    void returnsOnlyTheCallersOwnTenantsEntries() throws Exception {
        mockMvc.perform(get("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("ezzat").claim("tenant", "acme"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reference").value("LDG-1001"))
                .andExpect(jsonPath("$[0].tenant").value("acme"))
                .andExpect(jsonPath("$[0].amount").value(250.00))
                .andExpect(jsonPath("$[0].currency").value("EGP"))
                .andExpect(jsonPath("$[1].reference").value("LDG-1002"));
    }

    /** The same endpoint, a different tenant, a disjoint result — isolation in both directions. */
    @Test
    void aDifferentTenantSeesADifferentSetEntirely() throws Exception {
        mockMvc.perform(get("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("alice").claim("tenant", "default"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value("LDG-1003"))
                .andExpect(jsonPath("$[0].tenant").value("default"));
    }

    /**
     * A client-credentials token has no user and so no tenant claim. It must see nothing
     * rather than everything — the failure mode here should be empty, not total exposure.
     */
    @Test
    void aTokenWithNoTenantClaimSeesNothing() throws Exception {
        mockMvc.perform(get("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("authcore-machine"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
