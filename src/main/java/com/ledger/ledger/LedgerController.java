package com.ledger.ledger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final LedgerRepository repository;

    public LedgerController(LedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/entries")
    public List<LedgerEntry> entries() {
        return repository.findAll();
    }
}
