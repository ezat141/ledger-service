package com.ledger.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks")
@AutoConfigureMockMvc
class WhoAmITest {

    @Autowired
    MockMvc mockMvc;

    /** Bypassing the gateway: the token authenticates, and no X-GK-* headers arrive. */
    @Test
    void derivesIdentityFromTheTokenWhenCalledDirectly() throws Exception {
        mockMvc.perform(get("/ledger/whoami")
                        .with(jwt().jwt(jwt -> jwt.subject("ezzat").claim("tenant", "acme"))
                                  .authorities(new SimpleGrantedAuthority("payments:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromToken.subject").value("ezzat"))
                .andExpect(jsonPath("$.fromToken.tenant").value("acme"))
                .andExpect(jsonPath("$.fromHeaders.subject").doesNotExist())
                .andExpect(jsonPath("$.match").value(false));
    }

    /** Through the gateway: the stamped headers agree with the token. */
    @Test
    void reportsAMatchWhenHeadersAgreeWithTheToken() throws Exception {
        mockMvc.perform(get("/ledger/whoami")
                        .with(jwt().jwt(jwt -> jwt.subject("ezzat").claim("tenant", "acme"))
                                  .authorities(new SimpleGrantedAuthority("payments:read")))
                        .header("X-GK-Subject", "ezzat")
                        .header("X-GK-Tenant", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match").value(true));
    }

    /** Headers disagreeing with the token is the case this endpoint exists to surface. */
    @Test
    void reportsAMismatchWhenHeadersContradictTheToken() throws Exception {
        mockMvc.perform(get("/ledger/whoami")
                        .with(jwt().jwt(jwt -> jwt.subject("ezzat").claim("tenant", "acme")))
                        .header("X-GK-Subject", "admin")
                        .header("X-GK-Tenant", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromToken.tenant").value("acme"))
                .andExpect(jsonPath("$.fromHeaders.tenant").value("default"))
                .andExpect(jsonPath("$.match").value(false));
    }

    /**
     * A client-credentials token has no user and so no tenant claim. This must still
     * answer — reporting the absence — rather than refusing. Refusing here would blind
     * the one endpoint whose job is diagnosing incomplete identity.
     */
    @Test
    void reportsMissingTenantRatherThanRefusing() throws Exception {
        mockMvc.perform(get("/ledger/whoami")
                        .with(jwt().jwt(jwt -> jwt.subject("authcore-machine"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromToken.subject").value("authcore-machine"))
                .andExpect(jsonPath("$.fromToken.tenant").doesNotExist())
                .andExpect(jsonPath("$.match").value(false));
    }

    /**
     * Identity headers alone must never authenticate anyone — they are informational,
     * and this service does not trust them.
     */
    @Test
    void refusesHeadersWithoutABearerToken() throws Exception {
        mockMvc.perform(get("/ledger/whoami")
                        .header("X-GK-Subject", "admin")
                        .header("X-GK-Tenant", "default")
                        .header("X-GK-Permissions", "payments:write"))
                .andExpect(status().isUnauthorized());
    }
}
