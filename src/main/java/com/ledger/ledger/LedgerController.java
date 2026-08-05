package com.ledger.ledger;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final LedgerRepository repository;

    public LedgerController(LedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/entries")
    public List<LedgerEntry> entries(@AuthenticationPrincipal Jwt jwt) {
        return repository.findByTenant(jwt.getClaimAsString("tenant"));
    }

    /**
     * Enforced here, in the service that owns the data — not only at the gateway.
     * GateKeeper's route-level rules (M4) are a coarse outer layer; this is the
     * authoritative check, and it still applies when the gateway is bypassed.
     *
     * <p>The tenant is taken from the verified token, never from the request body, so a
     * caller cannot write an entry into someone else's tenant. A token with no tenant
     * claim — every client-credentials token, since AuthCore omits the claim when there
     * is no user — is refused rather than accepted, because reads are tenant-scoped and
     * such a write would be unreadable by everyone, including its own author.
     */
    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payments:write')")
    public LedgerEntry create(@RequestBody NewEntryRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        String tenant = jwt.getClaimAsString("tenant");
        if (tenant == null || tenant.isBlank()) {
            // A client-credentials token has no user and so no tenant. Accepting the
            // write would store a row no caller can ever read back — not even this one,
            // since reads are tenant-scoped. Refusing is the honest answer, and it
            // matches how the read path already treats a missing tenant.
            throw new AccessDeniedException("a tenant claim is required to write a ledger entry");
        }

        return repository.add(new LedgerEntry(
                request.reference(),
                tenant,
                request.amount(),
                request.currency(),
                Instant.now()));
    }

    /**
     * Reports both identities without judging either.
     *
     * <p>Deliberately does not apply {@code create(...)}'s missing-tenant guard. A write
     * with no tenant is refused because it would store an unreadable row; a whoami with
     * no tenant is the diagnosis, and refusing it would blind the endpoint at the moment
     * it is most useful.
     */
    @GetMapping("/whoami")
    public WhoAmI whoami(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-GK-Subject", required = false) String headerSubject,
            @RequestHeader(value = "X-GK-Tenant", required = false) String headerTenant,
            @RequestHeader(value = "X-GK-Permissions", required = false) String headerPermissions) {

        List<String> tokenPermissions = jwt.getClaimAsStringList("permissions");

        WhoAmI.Identity fromToken = new WhoAmI.Identity(
                jwt.getSubject(),
                jwt.getClaimAsString("tenant"),
                tokenPermissions == null ? List.of() : tokenPermissions);

        WhoAmI.Identity fromHeaders = (headerSubject == null && headerTenant == null)
                ? WhoAmI.Identity.EMPTY
                : new WhoAmI.Identity(headerSubject, headerTenant, splitPermissions(headerPermissions));

        return WhoAmI.of(fromToken, fromHeaders);
    }

    private static List<String> splitPermissions(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
}
