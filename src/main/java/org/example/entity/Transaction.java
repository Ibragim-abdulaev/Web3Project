package org.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public Wallet getFromWallet() { return fromWallet; }
    public void setFromWallet(Wallet fromWallet) { this.fromWallet = fromWallet; }

    public Wallet getToWallet() { return toWallet; }
    public void setToWallet(Wallet toWallet) { this.toWallet = toWallet; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getGasPrice() { return gasPrice; }
    public void setGasPrice(BigDecimal gasPrice) { this.gasPrice = gasPrice; }

    public BigDecimal getGasUsed() { return gasUsed; }
    public void setGasUsed(BigDecimal gasUsed) { this.gasUsed = gasUsed; }

    public BigDecimal getGasFee() { return gasFee; }
    public void setGasFee(BigDecimal gasFee) { this.gasFee = gasFee; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Wallet.NetworkType getNetwork() { return network; }
    public void setNetwork(Wallet.NetworkType network) { this.network = network; }

    public Long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Long blockNumber) { this.blockNumber = blockNumber; }

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public Integer getConfirmationCount() { return confirmationCount; }
    public void setConfirmationCount(Integer confirmationCount) { this.confirmationCount = confirmationCount; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public String getTokenSymbol() { return tokenSymbol; }
    public void setTokenSymbol(String tokenSymbol) { this.tokenSymbol = tokenSymbol; }

    public Integer getTokenDecimals() { return tokenDecimals; }
    public void setTokenDecimals(Integer tokenDecimals) { this.tokenDecimals = tokenDecimals; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getBlockchainTimestamp() { return blockchainTimestamp; }
    public void setBlockchainTimestamp(LocalDateTime blockchainTimestamp) { this.blockchainTimestamp = blockchainTimestamp; }

    public enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        CANCELLED
    }

    public enum TransactionType {
        TRANSFER,
        CONTRACT_CALL,
        CONTRACT_DEPLOYMENT,
        NFT_MINT,
        NFT_TRANSFER,
        TOKEN_TRANSFER,
        SWAP
    }
}
