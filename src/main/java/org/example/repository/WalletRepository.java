package org.example.repository;


import org.example.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByAddress(String address);

    List<Wallet> findByUserId(Long userId);

    List<Wallet> findByUserIdAndIsActive(Long userId, Boolean isActive);

    List<Wallet> findByNetworkAndIsActive(Wallet.NetworkType network, Boolean isActive);

    Boolean existsByAddress(String address);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.network = :network AND w.isActive = true")
    List<Wallet> findActiveWalletsByUserAndNetwork(@Param("userId") Long userId, @Param("network") Wallet.NetworkType network);

    @Query("SELECT w FROM Wallet w LEFT JOIN FETCH w.sentTransactions LEFT JOIN FETCH w.receivedTransactions WHERE w.address = :address")
    Optional<Wallet> findByAddressWithTransactions(@Param("address") String address);

    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.network = :network")
    Long countByNetwork(@Param("network") Wallet.NetworkType network);
}
