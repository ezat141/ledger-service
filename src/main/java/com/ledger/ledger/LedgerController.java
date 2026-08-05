package com.ledger.ledger;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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
}
