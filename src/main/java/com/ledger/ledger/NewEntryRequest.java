package com.ledger.ledger;

import java.math.BigDecimal;

public record NewEntryRequest(String reference, BigDecimal amount, String currency) {
}
