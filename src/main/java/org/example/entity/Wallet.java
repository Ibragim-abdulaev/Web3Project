package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "address", unique = true, nullable = false)
    private String address;

    @Column(name = "private_key")
    @JsonIgnore
    private String privateKey;

    @Column(name = "balance", precision = 36, scale = 18)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false)
    private NetworkType network;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public NetworkType getNetwork() { return network; }
    public void setNetwork(NetworkType network) { this.network = network; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Set<Transaction> getSentTransactions() { return sentTransactions; }
    public void setSentTransactions(Set<Transaction> sentTransactions) { this.sentTransactions = sentTransactions; }

    public Set<Transaction> getReceivedTransactions() { return receivedTransactions; }
    public void setReceivedTransactions(Set<Transaction> receivedTransactions) { this.receivedTransactions = receivedTransactions; }

    public Set<NFT> getNfts() { return nfts; }
    public void setNfts(Set<NFT> nfts) { this.nfts = nfts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum NetworkType {
        ETHEREUM,
        POLYGON,
        BINANCE_SMART_CHAIN,
        AVALANCHE,
        ARBITRUM,
        OPTIMISM
    }
}
