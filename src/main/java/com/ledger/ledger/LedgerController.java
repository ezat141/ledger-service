package com.ledger.ledger;

import org.springframework.http.HttpStatus;
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
     * caller cannot write an entry into someone else's tenant.
     */
    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payments:write')")
    public LedgerEntry create(@RequestBody NewEntryRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        return repository.add(new LedgerEntry(
                request.reference(),
                jwt.getClaimAsString("tenant"),
                request.amount(),
                request.currency(),
                Instant.now()));
    }
}
