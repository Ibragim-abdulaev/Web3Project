package org.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "nfts")
public class NFT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "token_id", nullable = false)
    private String tokenId;

    @NotBlank
    @Column(name = "contract_address", nullable = false)
    private String contractAddress;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "metadata_uri")
    private String metadataUri;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false)
    private Wallet.NetworkType network;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(name = "owner_address", nullable = false)
    private String ownerAddress;

    @Column(name = "creator_address")
    private String creatorAddress;

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "collection_symbol")
    private String collectionSymbol;

    @Column(name = "price", precision = 36, scale = 18)
    private BigDecimal price;

    @Column(name = "currency")
    private String currency;

    @Column(name = "is_for_sale", nullable = false)
    private Boolean isForSale = false;

    @Column(name = "royalty_percentage", precision = 5, scale = 2)
    private BigDecimal royaltyPercentage;

    @Column(name = "royalty_recipient")
    private String royaltyRecipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false)
    private TokenStandard standard = TokenStandard.ERC721;

    @Column(name = "rarity_rank")
    private Integer rarityRank;

    @Column(name = "rarity_score", precision = 10, scale = 4)
    private BigDecimal rarityScore;

    @Column(name = "last_sale_price", precision = 36, scale = 18)
    private BigDecimal lastSalePrice;

    @Column(name = "last_sale_date")
    private LocalDateTime lastSaleDate;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NFT() {}

    public NFT(String tokenId, String contractAddress, Wallet.NetworkType network, String ownerAddress) {
        this.tokenId = tokenId;
        this.contractAddress = contractAddress;
        this.network = network;
        this.ownerAddress = ownerAddress;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public String getMetadataUri() { return metadataUri; }
    public void setMetadataUri(String metadataUri) { this.metadataUri = metadataUri; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Wallet.NetworkType getNetwork() { return network; }
    public void setNetwork(Wallet.NetworkType network) { this.network = network; }

    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }

    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }

    public String getCreatorAddress() { return creatorAddress; }
    public void setCreatorAddress(String creatorAddress) { this.creatorAddress = creatorAddress; }

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }

    public String getCollectionSymbol() { return collectionSymbol; }
    public void setCollectionSymbol(String collectionSymbol) { this.collectionSymbol = collectionSymbol; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getIsForSale() { return isForSale; }
    public void setIsForSale(Boolean isForSale) { this.isForSale = isForSale; }

    public BigDecimal getRoyaltyPercentage() { return royaltyPercentage; }
    public void setRoyaltyPercentage(BigDecimal royaltyPercentage) { this.royaltyPercentage = royaltyPercentage; }

    public String getRoyaltyRecipient() { return royaltyRecipient; }
    public void setRoyaltyRecipient(String royaltyRecipient) { this.royaltyRecipient = royaltyRecipient; }

    public TokenStandard getStandard() { return standard; }
    public void setStandard(TokenStandard standard) { this.standard = standard; }

    public Integer getRarityRank() { return rarityRank; }
    public void setRarityRank(Integer rarityRank) { this.rarityRank = rarityRank; }

    public BigDecimal getRarityScore() { return rarityScore; }
    public void setRarityScore(BigDecimal rarityScore) { this.rarityScore = rarityScore; }

    public BigDecimal getLastSalePrice() { return lastSalePrice; }
    public void setLastSalePrice(BigDecimal lastSalePrice) { this.lastSalePrice = lastSalePrice; }

    public LocalDateTime getLastSaleDate() { return lastSaleDate; }
    public void setLastSaleDate(LocalDateTime lastSaleDate) { this.lastSaleDate = lastSaleDate; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum TokenStandard {
        ERC721,
        ERC1155
    }
}
