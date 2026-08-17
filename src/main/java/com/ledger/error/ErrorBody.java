package com.ledger.error;

import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The one JSON error shape this service renders for a refused or failed request, matching the
 * shape gatekeeper's own {@code ErrorBody} renders one hop upstream for the same failures —
 * {@code {"error": "forbidden", "status": 403, "path": "/ledger/entries"}}. Duplicated
 * deliberately: the two services share a wire contract, not code.
 *
 * <p>Used by both {@link JsonAccessDeniedHandler} (403: authenticated but refused) and {@link
 * JsonAuthenticationEntryPoint} (401: no valid credential at all). One construction site for
 * one shape, so the two cannot quietly drift apart.
 *
 * <p>{@code detail} is optional and, when present, must be a message this service authored
 * itself — never a framework exception's message, which risks leaking internals the caller has
 * no business seeing.
 */
final class ErrorBody {

    private ErrorBody() {
    }

    static Map<String, Object> of(HttpStatus status, String path) {
        return of(status, path, null);
    }

    static Map<String, Object> of(HttpStatus status, String path, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", status.getReasonPhrase().toLowerCase(Locale.ROOT).replace(' ', '_'));
        body.put("status", status.value());
        body.put("path", path);
        if (detail != null) {
            body.put("detail", detail);
        }
        return body;
    }
}
