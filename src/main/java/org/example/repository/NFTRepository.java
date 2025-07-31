package org.example.repository;

import org.example.entity.NFT;
import org.example.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NFTRepository extends JpaRepository<NFT, Long> {

    Optional<NFT> findByTokenIdAndContractAddress(String tokenId, String contractAddress);

    List<NFT> findByOwnerAddress(String ownerAddress);

    List<NFT> findByOwnerAddressAndNetwork(String ownerAddress, Wallet.NetworkType network);

    List<NFT> findByContractAddress(String contractAddress);

    List<NFT> findByCollectionName(String collectionName);

    List<NFT> findByIsForSale(Boolean isForSale);

    Page<NFT> findByOwnerAddressOrderByCreatedAtDesc(String ownerAddress, Pageable pageable);

    Page<NFT> findByIsForSaleOrderByPriceAsc(Boolean isForSale, Pageable pageable);

    @Query("SELECT n FROM NFT n WHERE n.name LIKE %:keyword% OR n.description LIKE %:keyword% OR n.collectionName LIKE %:keyword%")
    Page<NFT> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM NFT n WHERE n.network = :network AND n.isVerified = true ORDER BY n.likeCount DESC")
    List<NFT> findPopularNFTsByNetwork(@Param("network") Wallet.NetworkType network, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NFT n WHERE n.ownerAddress = :ownerAddress")
    Long countByOwnerAddress(@Param("ownerAddress") String ownerAddress);

    @Query("SELECT COUNT(n) FROM NFT n WHERE n.contractAddress = :contractAddress")
    Long countByContractAddress(@Param("contractAddress") String contractAddress);

    @Query("SELECT DISTINCT n.collectionName FROM NFT n WHERE n.collectionName IS NOT NULL ORDER BY n.collectionName")
    List<String> findAllCollections();

    @Query("SELECT n FROM NFT n WHERE n.rarityRank IS NOT NULL ORDER BY n.rarityRank ASC")
    List<NFT> findByRarityRankOrderByRankAsc(Pageable pageable);

    @Query(value = "SELECT * FROM nfts WHERE owner_address = :ownerAddress ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<NFT> findLatestNFTsByOwner(@Param("ownerAddress") String ownerAddress, @Param("limit") int limit);
}
