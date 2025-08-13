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
@Table(name = "nfts")
@Getter
@Setter
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

    public enum TokenStandard {
        ERC721,
        ERC1155
    }
}
