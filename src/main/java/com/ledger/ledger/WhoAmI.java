package com.ledger.ledger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The identity as this service derived it from the verified token, beside the identity
 * GateKeeper asserted in headers.
 *
 * <p>Called through the gateway these agree. Called directly on :8082 the header side is
 * empty while the token side is populated — one response showing both that the headers
 * are not authoritative and that this service is secure without the gateway in front.
 *
 * <p>Nothing here refuses an incomplete identity. A token with no tenant, or headers that
 * contradict the token, is precisely what this endpoint exists to reveal; answering with
 * a 403 would hide it.
 *
 * <p>{@code match} covers subject, tenant <b>and</b> permissions. Permissions are compared
 * as sets: {@code X-GK-Permissions} is a comma-joined string with no meaningful order, so
 * two identities carrying the same permissions in a different sequence are still the same
 * identity. Omitting permissions from the comparison would let forged headers that assert
 * extra authority (or drop required authority) read as a match, which is precisely the
 * disagreement this endpoint exists to surface.
 */
public record WhoAmI(Identity fromToken, Identity fromHeaders, boolean match) {

    public record Identity(String subject, String tenant, List<String> permissions) {

        @JsonIgnore
        public boolean isEmpty() {
            return subject == null && tenant == null && permissions.isEmpty();
        }
    }

    public static WhoAmI of(Identity fromToken, Identity fromHeaders) {
        boolean match = !fromHeaders.isEmpty()
                && Objects.equals(fromToken.subject(), fromHeaders.subject())
                && Objects.equals(fromToken.tenant(), fromHeaders.tenant())
                && Set.copyOf(fromToken.permissions()).equals(Set.copyOf(fromHeaders.permissions()));
        return new WhoAmI(fromToken, fromHeaders, match);
    }
}
