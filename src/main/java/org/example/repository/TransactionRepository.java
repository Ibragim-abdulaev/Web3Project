package org.example.repository;

import org.example.entity.Transaction;
import org.example.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByHash(String hash);

    List<Transaction> findByFromAddress(String fromAddress);

    List<Transaction> findByToAddress(String toAddress);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByNetwork(Wallet.NetworkType network);

    Page<Transaction> findByFromAddressOrToAddress(String fromAddress, String toAddress, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromAddress = :address OR t.toAddress = :address ORDER BY t.createdAt DESC")
    Page<Transaction> findByAddressOrderByCreatedAtDesc(@Param("address") String address, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt < :timestamp")
    List<Transaction> findPendingTransactionsOlderThan(@Param("status") Transaction.TransactionStatus status,
                                                       @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    Long countByStatus(@Param("status") Transaction.TransactionStatus status);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.fromAddress = :address AND t.status = 'CONFIRMED'")
    BigDecimal getTotalSentAmount(@Param("address") String address);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.toAddress = :address AND t.status = 'CONFIRMED'")
    BigDecimal getTotalReceivedAmount(@Param("address") String address);

    @Query("SELECT t FROM Transaction t WHERE t.network = :network AND t.type = :type AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByNetworkAndTypeAndDateRange(@Param("network") Wallet.NetworkType network,
                                                       @Param("type") Transaction.TransactionType type,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT * FROM transactions WHERE network = :network ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Transaction> findLatestTransactionsByNetwork(@Param("network") String network, @Param("limit") int limit);
}