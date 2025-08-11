package org.example.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.entity.Transaction;
import org.example.entity.Wallet;
import org.example.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transaction Management", description = "Operations related to blockchain transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/address/{address}")
    @Operation(summary = "Get transactions by address", description = "Get all transactions for a specific address")
    public ResponseEntity<?> getTransactionsByAddress(
            @PathVariable String address,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getTransactionsByAddress(address, pageable);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{hash}")
    @Operation(summary = "Get transaction by hash", description = "Get transaction details by hash")
    public ResponseEntity<?> getTransactionByHash(@PathVariable String hash) {
        return transactionService.findByHash(hash)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest/{network}")
    @Operation(summary = "Get latest transactions", description = "Get latest transactions by network")
    public ResponseEntity<?> getLatestTransactions(
            @PathVariable Wallet.NetworkType network,
            @RequestParam(defaultValue = "10") int limit) {

        List<Transaction> transactions = transactionService.getLatestTransactionsByNetwork(network, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        response.put("network", network);
        response.put("count", transactions.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get transaction statistics", description = "Get overall transaction statistics")
    public ResponseEntity<?> getTransactionStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pendingCount", transactionService.getTransactionCountByStatus(Transaction.TransactionStatus.PENDING));
        stats.put("confirmedCount", transactionService.getTransactionCountByStatus(Transaction.TransactionStatus.CONFIRMED));
        stats.put("failedCount", transactionService.getTransactionCountByStatus(Transaction.TransactionStatus.FAILED));

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{hash}/update-status")
    @Operation(summary = "Update transaction status", description = "Manually update transaction status")
    public ResponseEntity<?> updateTransactionStatus(@PathVariable String hash) {
        transactionService.updateTransactionStatus(hash);
        return ResponseEntity.ok(Map.of("message", "Transaction status update initiated"));
    }
}