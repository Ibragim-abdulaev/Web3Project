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
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_hash", columnList = "hash"),
        @Index(name = "idx_transaction_from", columnList = "from_address"),
        @Index(name = "idx_transaction_to", columnList = "to_address"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_network", columnList = "network"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_created", columnList = "created_at")
})
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "hash", unique = true, nullable = false, length = 66)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @NotBlank
    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @NotBlank
    @Column(name = "to_address", nullable = false, length = 42)
    private String toAddress;

    @NotNull
    @Column(name = "amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal amount;

    @Column(name = "gas_price", precision = 36, scale = 18)
    private BigDecimal gasPrice;

    @Column(name = "gas_limit", precision = 10, scale = 0)
    private BigDecimal gasLimit;

    @Column(name = "gas_used", precision = 10, scale = 0)
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

    @Column(name = "block_hash", length = 66)
    private String blockHash;

    @Column(name = "transaction_index")
    private Integer transactionIndex;

    @Column(name = "confirmation_count")
    private Integer confirmationCount = 0;

    @Column(name = "contract_address", length = 42)
    private String contractAddress;

    @Column(name = "token_symbol")
    private String tokenSymbol;

    @Column(name = "token_decimals")
    private Integer tokenDecimals = 18;

    @Column(name = "token_amount", precision = 36, scale = 18)
    private BigDecimal tokenAmount;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "memo")
    private String memo;

    @Column(name = "internal_id")
    private String internalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private TransactionPriority priority = TransactionPriority.NORMAL;

    @Column(name = "estimated_confirmation_time")
    private LocalDateTime estimatedConfirmationTime;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "blockchain_timestamp")
    private LocalDateTime blockchainTimestamp;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

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
        PENDING("Pending confirmation"),
        CONFIRMED("Confirmed"),
        FAILED("Transaction failed"),
        CANCELLED("Cancelled"),
        DROPPED("Dropped from mempool"),
        REPLACED("Replaced by another transaction");

        private final String description;
        TransactionStatus(String description) {
            this.description = description;
        }
        public String getDescription() { return description; }
    }

    public enum TransactionType {
        TRANSFER("Native token transfer"),
        TOKEN_TRANSFER("ERC-20 token transfer"),
        CONTRACT_INTERACTION("Smart contract interaction"),
        NFT_TRANSFER("NFT transfer"),
        DEPOSIT("Deposit"),
        WITHDRAWAL("Withdrawal"),
        SWAP("Token swap"),
        STAKE("Staking"),
        UNSTAKE("Unstaking");

        private final String description;
        TransactionType(String description) {
            this.description = description;
        }
        public String getDescription() { return description; }
    }

    public enum TransactionPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        URGENT(4);

        private final int level;
        TransactionPriority(int level) {
            this.level = level;
        }
        public int getLevel() { return level; }
    }

    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED;
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean canRetry() {
        return retryCount < maxRetries &&
                (status == TransactionStatus.FAILED || status == TransactionStatus.DROPPED);
    }

    public void incrementRetry() {
        this.retryCount++;
        this.nextRetryAt = LocalDateTime.now().plusMinutes((long) Math.pow(2, retryCount));
    }

    public BigDecimal getTotalCost() {
        BigDecimal fee = gasFee != null ? gasFee : BigDecimal.ZERO;
        return amount.add(fee);
    }

    public boolean isTokenTransfer() {
        return contractAddress != null && !contractAddress.isEmpty();
    }

    public String getDisplayAmount() {
        if (isTokenTransfer() && tokenAmount != null) {
            return tokenAmount.toPlainString() + " " + (tokenSymbol != null ? tokenSymbol : "TOKEN");
        }
        return amount.toPlainString() + " " + network.getNativeCurrency();
    }
}
