package com.ledger.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(
        String reference,
        String tenant,
        BigDecimal amount,
        String currency,
        Instant postedAt) {
}
