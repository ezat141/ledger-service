package com.ledger.error;

import com.ledger.ledger.TenantRequiredException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders the platform's JSON error shape for a 403, replacing Spring Security's default
 * {@code BearerTokenAccessDeniedHandler} — which leaves the response body empty and, worse,
 * stamps every {@code AccessDeniedException} on the {@code WWW-Authenticate} header as {@code
 * error="insufficient_scope"} regardless of why access was actually denied.
 *
 * <p>That label is actively wrong for {@link TenantRequiredException}: the caller already
 * holds the permission the endpoint requires, and no amount of additional scope can supply a
 * {@code tenant} claim the token structurally does not carry. Telling them otherwise sends
 * whoever is debugging it after a fix that cannot work. This handler tells the two causes
 * apart by exception type and describes each honestly, and — matching gatekeeper's contract,
 * which never sets {@code WWW-Authenticate} on a 403 at all — sets no such header here either.
 *
 * <p>{@code detail} is always one of the two fixed strings below, both authored by this
 * service. Neither echoes {@code accessDeniedException.getMessage()}: that message is a
 * framework/application string never meant for a wire contract, and passing it through would
 * risk leaking internals to the caller.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MISSING_PERMISSION_DETAIL =
            "the token does not carry the permission this operation requires";
    private static final String MISSING_TENANT_DETAIL =
            "the token carries no tenant claim, so this write has no owner";

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String detail = accessDeniedException instanceof TenantRequiredException
                ? MISSING_TENANT_DETAIL
                : MISSING_PERMISSION_DETAIL;

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ErrorBody.of(HttpStatus.FORBIDDEN, request.getRequestURI(), detail));
    }
}
