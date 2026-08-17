package com.ledger.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders the platform's JSON error shape for a 401, replacing Spring Security's default
 * {@code BearerTokenAuthenticationEntryPoint}, which commits the response with an empty body.
 * Left at the default, this would be the one gap in an otherwise uniform error shape: JSON
 * everywhere except here.
 *
 * <p>{@code WWW-Authenticate: Bearer} is preserved — RFC 6750 requires it on a 401 from a
 * bearer-token resource, and adding the JSON body must not cost the platform that header. No
 * {@code error="..."} parameter is added: unlike the 403 case this class does not attempt to
 * describe why authentication failed (an absent token and a malformed one both land here), and
 * a bare {@code Bearer} challenge is what gatekeeper's own entry point sends for the same
 * failure one hop upstream.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ErrorBody.of(HttpStatus.UNAUTHORIZED, request.getRequestURI()));
    }
}
