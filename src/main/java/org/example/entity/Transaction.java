package org.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "hash", unique = true, nullable = false)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @NotBlank
    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @NotBlank
    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @NotNull
    @Column(name = "amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal amount;

    @Column(name = "gas_price", precision = 36, scale = 18)
    private BigDecimal gasPrice;

    @Column(name = "gas_used", precision = 36, scale = 18)
    private BigDecimal gasUsed;

    @Column(name = "gas_fee", precision = 36, scale = 18)
    private BigDecimal gasFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false)
    private Wallet.NetworkType network;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "confirmation_count")
    private Integer confirmationCount = 0;

    @Column(name = "contract_address")
    private String contractAddress;

    @Column(name = "token_symbol")
    private String tokenSymbol;

    @Column(name = "token_decimals")
    private Integer tokenDecimals;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "blockchain_timestamp")
    private LocalDateTime blockchainTimestamp;

    public Transaction() {}

    public Transaction(String hash, String fromAddress, String toAddress,
                       BigDecimal amount, TransactionType type, Wallet.NetworkType network) {
        this.hash = hash;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.type = type;
        this.network = network;
    }

    public enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        CANCELLED
    }

    public enum TransactionType {
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL
    }
}
