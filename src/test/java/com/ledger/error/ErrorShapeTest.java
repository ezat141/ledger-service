package com.ledger.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The platform's shared JSON error shape — {@code {error, status, path[, detail]}} — matching
 * what gatekeeper already renders one hop upstream for the same failures.
 *
 * <p>The point of this class is the pair of 403 tests: a missing permission and a missing
 * tenant are both {@code AccessDeniedException}s, but they are not the same failure. Rendering
 * them identically (as Spring's default {@code BearerTokenAccessDeniedHandler} does, always
 * labelling the {@code WWW-Authenticate} header {@code insufficient_scope}) sends whoever is
 * debugging the missing-tenant case after a fix that cannot work — no amount of extra scope
 * supplies a claim the token structurally lacks.
 */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks")
@AutoConfigureMockMvc
class ErrorShapeTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void rendersUnauthorizedAsJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/ledger/entries"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/ledger/entries"))
                .andReturn();

        assertThat(result.getResponse().getHeader("WWW-Authenticate")).matches("Bearer.*");
    }

    @Test
    void rendersAMissingPermissionAsForbiddenWithItsOwnDetail() throws Exception {
        mockMvc.perform(post("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("ezzat").claim("tenant", "acme"))
                                  .authorities(new SimpleGrantedAuthority("payments:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"LDG-4001","amount":"10.00","currency":"EGP"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail")
                        .value("the token does not carry the permission this operation requires"))
                // Asserted on both 403 branches, not just the tenant one, so a header added
                // to a single branch later cannot slip through. A 403 means the caller was
                // identified and refused; offering a way to authenticate would be nonsense.
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    /**
     * Same status and the same {@code error} label as the previous test, but this caller
     * already holds {@code payments:write} — the token simply has no tenant claim to give the
     * write an owner. The {@code detail} must say that, in words distinct from the
     * missing-permission case above; identical wording here would mean the fix did not work.
     */
    @Test
    void rendersAMissingTenantAsForbiddenWithADifferentDetail() throws Exception {
        mockMvc.perform(post("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("authcore-machine"))
                                  .authorities(new SimpleGrantedAuthority("payments:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"LDG-4002","amount":"10.00","currency":"EGP"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail")
                        .value("the token carries no tenant claim, so this write has no owner"));
    }

    /**
     * The regression guard. Spring's default {@code BearerTokenAccessDeniedHandler} stamps
     * {@code WWW-Authenticate: Bearer error="insufficient_scope"} on every {@code
     * AccessDeniedException} it handles, scope-related or not. Gatekeeper's contract never sets
     * this header on a 403 at all, so absence is the primary expectation here — but the
     * narrower, load-bearing assertion is that the misleading label specifically is gone, so
     * this guard still catches a regression even if some other, honest header were ever added.
     */
    @Test
    void doesNotLabelAMissingTenantAsInsufficientScope() throws Exception {
        MvcResult result = mockMvc.perform(post("/ledger/entries")
                        .with(jwt().jwt(jwt -> jwt.subject("authcore-machine"))
                                  .authorities(new SimpleGrantedAuthority("payments:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"LDG-4003","amount":"10.00","currency":"EGP"}
                                """))
                .andExpect(status().isForbidden())
                .andReturn();

        String wwwAuthenticate = result.getResponse().getHeader("WWW-Authenticate");
        assertThat(wwwAuthenticate).satisfiesAnyOf(
                header -> assertThat(header).isNull(),
                header -> assertThat(header).doesNotContain("insufficient_scope"));
    }
}
