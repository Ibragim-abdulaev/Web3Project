package org.example.repository;


import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByWalletAddress(String walletAddress);

    Optional<User> findByVerificationToken(String verificationToken);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Boolean existsByWalletAddress(String walletAddress);

    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = true")
    Long countVerifiedUsers();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallets WHERE u.id = :id")
    Optional<User> findByIdWithWallets(@Param("id") Long id);
}
