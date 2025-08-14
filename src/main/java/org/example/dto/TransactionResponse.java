package org.example.dto;

import org.example.entity.Transaction;
import org.example.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private String txHash;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private Wallet.NetworkType network;
    private Transaction.TransactionStatus status;
    private LocalDateTime createdAt;

    public TransactionResponse(String txHash, String fromAddress, String toAddress,
                               BigDecimal amount, Wallet.NetworkType network,
                               Transaction.TransactionStatus status, LocalDateTime createdAt) {
        this.txHash = txHash;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.network = network;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getTxHash() { return txHash; }
    public String getFromAddress() { return fromAddress; }
    public String getToAddress() { return toAddress; }
    public BigDecimal getAmount() { return amount; }
    public Wallet.NetworkType getNetwork() { return network; }
    public Transaction.TransactionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}