package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "nfts", indexes = {
        @Index(name = "idx_nft_owner", columnList = "owner_address"),
        @Index(name = "idx_nft_contract", columnList = "contract_address"),
        @Index(name = "idx_nft_collection", columnList = "collection_name"),
        @Index(name = "idx_nft_sale", columnList = "is_for_sale"),
        @Index(name = "idx_nft_network", columnList = "network"),
        @Index(name = "idx_nft_rarity", columnList = "rarity_rank")
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
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

    @URL(message = "Invalid image URL format")
    @Column(name = "image_url")
    private String imageUrl;

    @URL(message = "Invalid external URL format")
    @Column(name = "external_url")
    private String externalUrl;

    @URL(message = "Invalid metadata URI format")
    @Column(name = "metadata_uri")
    private String metadataUri;

    @OneToMany(mappedBy = "nft", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<NFTOwnershipHistory> ownershipHistory = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "nft_attributes",
            joinColumns = @JoinColumn(name = "nft_id"))
    @MapKeyColumn(name = "trait_type")
    @Column(name = "trait_value")
    private Map<String, String> attributes = new HashMap<>();

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

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status")
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

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

    public enum ModerationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        FLAGGED
    }

    public void addOwnershipHistory(NFTOwnershipHistory history) {
        ownershipHistory.add(history);
        history.setNft(this);
    }

    public void removeOwnershipHistory(NFTOwnershipHistory history) {
        ownershipHistory.remove(history);
        history.setNft(null);
    }
}
