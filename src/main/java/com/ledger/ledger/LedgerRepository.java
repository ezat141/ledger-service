package com.ledger.ledger;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory on purpose. This milestone is about the edge, and a database would add
 * migration and Testcontainers work that proves nothing the gateway does not already
 * prove. Swapping this for JPA later touches no other class.
 */
@Repository
public class LedgerRepository {

    private final List<LedgerEntry> entries = new CopyOnWriteArrayList<>(List.of(
            new LedgerEntry("LDG-1001", "acme", new BigDecimal("250.00"), "EGP",
                    Instant.now().minus(3, ChronoUnit.DAYS)),
            new LedgerEntry("LDG-1002", "acme", new BigDecimal("74.50"), "EGP",
                    Instant.now().minus(2, ChronoUnit.DAYS)),
            new LedgerEntry("LDG-1003", "default", new BigDecimal("1200.00"), "USD",
                    Instant.now().minus(1, ChronoUnit.DAYS))));

    /**
     * Entries belonging to one tenant.
     *
     * <p>AuthCore pins every user token to the tenant it was issued for precisely so a
     * resource server can scope data without a lookup. Filtering here rather than at the
     * edge is deliberate: GateKeeper's checks are a coarse outer layer, and this service
     * must still be correct when it is called directly with the gateway bypassed.
     */
    public List<LedgerEntry> findByTenant(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return List.of();
        }
        return entries.stream().filter(entry -> tenant.equals(entry.tenant())).toList();
    }

    public LedgerEntry add(LedgerEntry entry) {
        entries.add(entry);
        return entry;
    }
}
