package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.entity.User;
import org.example.entity.Wallet;
import org.example.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallet Management", description = "Operations related to crypto wallets")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user's wallets", description = "Retrieve all wallets belonging to the authenticated user")
    public ResponseEntity<?> getMyWallets(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Wallet> wallets = walletService.findByUserId(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("wallets", wallets);
            response.put("count", wallets.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting user wallets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve wallets: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create new wallet", description = "Create a new crypto wallet for the authenticated user")
    public ResponseEntity<?> createWallet(
            @RequestBody @Valid CreateWalletRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Wallet wallet = walletService.createWallet(user, request.getNetwork());

            Map<String, Object> response = new HashMap<>();
            response.put("wallet", wallet);
            response.put("message", "Wallet created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error creating wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create wallet: " + e.getMessage()));
        }
    }

    @GetMapping("/{address}/balance")
    @Operation(summary = "Get wallet balance", description = "Get the current balance of a wallet")
    public ResponseEntity<?> getBalance(
            @Parameter(description = "Wallet address") @PathVariable String address,
            @Parameter(description = "Network type") @RequestParam Wallet.NetworkType network) {
        try {
            if (!walletService.isValidAddress(address)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid wallet address"));
            }

            BigDecimal balance = walletService.getBalance(address, network);

            Map<String, Object> response = new HashMap<>();
            response.put("address", address);
            response.put("balance", balance);
            response.put("network", network);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting balance for address: {}", address, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get balance: " + e.getMessage()));
        }
    }

    @PostMapping("/{address}/transfer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Send transaction", description = "Send cryptocurrency from one wallet to another")
    public ResponseEntity<?> sendTransaction(
            @Parameter(description = "Sender wallet address") @PathVariable String address,
            @RequestBody @Valid TransferRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Wallet> walletOpt = walletService.findByAddress(address);
            if (!walletOpt.isPresent() || !walletOpt.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Wallet access denied"));
            }

            Wallet wallet = walletOpt.get();

            CompletableFuture<String> txHashFuture = transactionService.sendTransaction(
                    address,
                    request.getToAddress(),
                    request.getAmount(),
                    request.getNetwork(),
                    wallet.getPrivateKey()
            );

            String txHash = txHashFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("transactionHash", txHash);
            response.put("fromAddress", address);
            response.put("toAddress", request.getToAddress());
            response.put("amount", request.getAmount());
            response.put("network", request.getNetwork());
            response.put("message", "Transaction sent successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error sending transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send transaction: " + e.getMessage()));
        }
    }

    @GetMapping("/{address}")
    @Operation(summary = "Get wallet details", description = "Get detailed information about a wallet")
    public ResponseEntity<?> getWalletDetails(@PathVariable String address) {
        try {
            Optional<Wallet> walletOpt = walletService.findByAddress(address);
            if (!walletOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Wallet wallet = walletOpt.get();

            BigDecimal balance = walletService.getBalance(address, wallet.getNetwork());

            BigDecimal totalSent = transactionService.getTotalSentAmount(address);
            BigDecimal totalReceived = transactionService.getTotalReceivedAmount(address);

            Map<String, Object> response = new HashMap<>();
            response.put("address", wallet.getAddress());
            response.put("network", wallet.getNetwork());
            response.put("balance", balance);
            response.put("isActive", wallet.getIsActive());
            response.put("createdAt", wallet.getCreatedAt());
            response.put("totalSent", totalSent);
            response.put("totalReceived", totalReceived);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting wallet details for address: {}", address, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get wallet details: " + e.getMessage()));
        }
    }

    @PutMapping("/{address}/deactivate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Deactivate wallet", description = "Deactivate a wallet (soft delete)")
    public ResponseEntity<?> deactivateWallet(
            @PathVariable String address,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Wallet> walletOpt = walletService.findByAddress(address);
            if (!walletOpt.isPresent() || !walletOpt.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Wallet access denied"));
            }

            walletService.deactivateWallet(address);

            return ResponseEntity.ok(Map.of("message", "Wallet deactivated successfully"));

        } catch (Exception e) {
            logger.error("Error deactivating wallet: {}", address, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate wallet: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get wallet statistics", description = "Get overall wallet statistics")
    public ResponseEntity<?> getWalletStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            for (Wallet.NetworkType network : Wallet.NetworkType.values()) {
                Long count = walletService.getWalletCountByNetwork(network);
                stats.put(network.name().toLowerCase() + "Count", count);
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting wallet stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get wallet stats: " + e.getMessage()));
        }
    }

    public static class CreateWalletRequest {
        @NotBlank
        private Wallet.NetworkType network;

        public Wallet.NetworkType getNetwork() { return network; }
        public void setNetwork(Wallet.NetworkType network) { this.network = network; }
    }

    public static class TransferRequest {
        @NotBlank
        private String toAddress;

        private BigDecimal amount;

        private Wallet.NetworkType network;

        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Wallet.NetworkType getNetwork() { return network; }
        public void setNetwork(Wallet.NetworkType network) { this.network = network; }
    }
}
