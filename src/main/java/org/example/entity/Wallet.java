package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_address", columnList = "address"),
        @Index(name = "idx_wallet_user", columnList = "user_id"),
        @Index(name = "idx_wallet_network", columnList = "network"),
        @Index(name = "idx_wallet_active", columnList = "is_active")
})
@Getter
@Setter
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "address", unique = true, nullable = false)
    private String address;

    @Column(name = "encrypted_private_key")
    @JsonIgnore
    private String encryptedPrivateKey;

    @Column(name = "encryption_iv")
    @JsonIgnore
    private String encryptionIv;

    @Column(name = "balance", precision = 36, scale = 18)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false)
    private NetworkType network;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false)
    private WalletType walletType = WalletType.HOT;

    @Column(name = "daily_limit", precision = 36, scale = 18)
    private BigDecimal dailyLimit;

    @Column(name = "daily_spent", precision = 36, scale = 18)
    private BigDecimal dailySpent = BigDecimal.ZERO;

    @Column(name = "daily_limit_reset_date")
    private LocalDateTime dailyLimitResetDate;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "fromWallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Transaction> sentTransactions = new HashSet<>();

    @OneToMany(mappedBy = "toWallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Transaction> receivedTransactions = new HashSet<>();

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<NFT> nfts = new HashSet<>();

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "transaction_count", nullable = false)
    private Long transactionCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Wallet() {}

    public Wallet(String address, NetworkType network, User user) {
        this.address = address;
        this.network = network;
        this.user = user;
    }

    public enum WalletType {
        HOT,
        WATCH,
        HARDWARE,
        IMPORTED
    }

    public enum NetworkType {
        ETHEREUM("ETH", 18),
        POLYGON("MATIC", 18),
        BINANCE_SMART_CHAIN("BNB", 18),
        AVALANCHE("AVAX", 18),
        ARBITRUM("ETH", 18),
        OPTIMISM("ETH", 18);

        private final String nativeCurrency;
        private final int decimals;

        NetworkType(String nativeCurrency, int decimals) {
            this.nativeCurrency = nativeCurrency;
            this.decimals = decimals;
        }

        public String getNativeCurrency() { return nativeCurrency; }
        public int getDecimals() { return decimals; }
    }

    public void incrementTransactionCount() {
        this.transactionCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean isWithinDailyLimit(BigDecimal amount) {
        if (dailyLimit == null) return true;

        resetDailyLimitIfNeeded();
        return dailySpent.add(amount).compareTo(dailyLimit) <= 0;
    }

    public void addToDailySpent(BigDecimal amount) {
        resetDailyLimitIfNeeded();
        this.dailySpent = this.dailySpent.add(amount);
    }

    private void resetDailyLimitIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (dailyLimitResetDate == null || now.isAfter(dailyLimitResetDate)) {
            this.dailySpent = BigDecimal.ZERO;
            this.dailyLimitResetDate = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    public String getDisplayName() {
        return label != null && !label.isEmpty() ? label :
                address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }
}
