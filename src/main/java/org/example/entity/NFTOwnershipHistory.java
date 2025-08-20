package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "nft_ownership_history", indexes = {
        @Index(name = "idx_history_nft", columnList = "nft_id"),
        @Index(name = "idx_history_owner", columnList = "owner_address")
})
@Getter
@Setter
public class NFTOwnershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nft_id", nullable = false)
    private NFT nft;

    @Column(name = "owner_address", nullable = false)
    private String ownerAddress;

    @Column(name = "price", precision = 36, scale = 18)
    private java.math.BigDecimal price;

    @Column(name = "currency")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.TRANSFER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EventType {
        MINT,
        TRANSFER,
        SALE,
        AUCTION
    }
}