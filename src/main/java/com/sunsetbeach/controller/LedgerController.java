package com.sunsetbeach.controller;

import com.sunsetbeach.api.LedgerApi;
import com.sunsetbeach.model.JournalEntry;
import com.sunsetbeach.model.JournalEntryCreateInput;
import com.sunsetbeach.model.JournalEntryReverseInput;
import com.sunsetbeach.model.LedgerAccount;
import com.sunsetbeach.model.LedgerAccountCreateInput;
import com.sunsetbeach.service.LedgerService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LedgerController implements LedgerApi {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    public ResponseEntity<List<LedgerAccount>> listLedgerAccounts() {
        return ResponseEntity.ok(ledgerService.listAccounts());
    }

    @Override
    public ResponseEntity<LedgerAccount> createLedgerAccount(LedgerAccountCreateInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.createAccount(input));
    }

    @Override
    public ResponseEntity<List<JournalEntry>> listJournalEntries(String from, String to) {
        return ResponseEntity.ok(ledgerService.listEntries(from, to));
    }

    @Override
    public ResponseEntity<JournalEntry> createJournalEntry(JournalEntryCreateInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.createManualEntry(input));
    }

    @Override
    public ResponseEntity<JournalEntry> reverseJournalEntry(String id, JournalEntryReverseInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.reverse(id, input));
    }
}
