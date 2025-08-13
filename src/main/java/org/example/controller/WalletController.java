package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.entity.User;
import org.example.entity.Wallet;
import org.example.service.TransactionService;
import org.example.service.UserService;
import org.example.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallet Management", description = "Operations related to crypto wallets")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final UserService userService;

    public WalletController(WalletService walletService,
                            TransactionService transactionService,
                            UserService userService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user's wallets")
    public ResponseEntity<?> getMyWallets(Authentication authentication) {
        User user = getUser(authentication);
        List<Wallet> wallets = walletService.findByUserId(user.getId());
        return ResponseEntity.ok(Map.of(
                "wallets", wallets,
                "count", wallets.size()
        ));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create new wallet")
    public ResponseEntity<?> createWallet(@RequestBody @Valid CreateWalletRequest request,
                                          Authentication authentication) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        User user = getUser(authentication);
        Wallet wallet = walletService.createWallet(user, request.getNetwork());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "wallet", wallet,
                "message", "Wallet created successfully"
        ));
    }

    @GetMapping("/{address}/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<?> getBalance(
            @Parameter(description = "Wallet address") @PathVariable String address,
            @Parameter(description = "Network type") @RequestParam Wallet.NetworkType network) {

        if (!walletService.isValidAddress(address)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid wallet address"));
        }

        BigDecimal balance = walletService.getBalance(address, network);
        return ResponseEntity.ok(Map.of(
                "address", address,
                "balance", balance,
                "network", network
        ));
    }

    @PostMapping("/{address}/transfer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Send transaction")
    public ResponseEntity<?> sendTransaction(
            @PathVariable String address,
            @RequestBody @Valid TransferRequest request,
            Authentication authentication) throws Exception {

        User user = getUser(authentication);
        Wallet wallet = getOwnedWalletOrThrow(address, user);

        String txHash = transactionService.sendTransaction(
                address,
                request.getToAddress(),
                request.getAmount(),
                request.getNetwork(),
                wallet.getPrivateKey()
        ).get();

        return ResponseEntity.ok(Map.of(
                "transactionHash", txHash,
                "fromAddress", address,
                "toAddress", request.getToAddress(),
                "amount", request.getAmount(),
                "network", request.getNetwork(),
                "message", "Transaction sent successfully"
        ));
    }

    @GetMapping("/{address}")
    @Operation(summary = "Get wallet details")
    public ResponseEntity<?> getWalletDetails(@PathVariable String address) {
        Wallet wallet = walletService.findByAddress(address)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal balance = walletService.getBalance(address, wallet.getNetwork());
        return ResponseEntity.ok(Map.of(
                "address", wallet.getAddress(),
                "network", wallet.getNetwork(),
                "balance", balance,
                "isActive", wallet.getIsActive(),
                "createdAt", wallet.getCreatedAt(),
                "totalSent", transactionService.getTotalSentAmount(address),
                "totalReceived", transactionService.getTotalReceivedAmount(address)
        ));
    }

    @PutMapping("/{address}/deactivate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Deactivate wallet")
    public ResponseEntity<?> deactivateWallet(@PathVariable String address,
                                              Authentication authentication) {
        User user = getUser(authentication);
        getOwnedWalletOrThrow(address, user);
        walletService.deactivateWallet(address);
        return ResponseEntity.ok(Map.of("message", "Wallet deactivated successfully"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get wallet statistics")
    public ResponseEntity<?> getWalletStats() {
        Map<String, Object> stats = new HashMap<>();
        for (Wallet.NetworkType network : Wallet.NetworkType.values()) {
            stats.put(network.name().toLowerCase() + "Count", walletService.getWalletCountByNetwork(network));
        }
        return ResponseEntity.ok(stats);
    }

    public static class CreateWalletRequest {
        @NotNull(message = "Network type is required")
        private Wallet.NetworkType network;

        public Wallet.NetworkType getNetwork() { return network; }
        public void setNetwork(Wallet.NetworkType network) { this.network = network; }
    }

    public static class TransferRequest {
        @NotBlank(message = "Destination address is required")
        private String toAddress;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00000001", message = "Amount must be greater than zero")
        private BigDecimal amount;

        @NotNull(message = "Network type is required")
        private Wallet.NetworkType network;

        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Wallet.NetworkType getNetwork() { return network; }
        public void setNetwork(Wallet.NetworkType network) { this.network = network; }
    }

    private User getUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Wallet getOwnedWalletOrThrow(String address, User user) {
        return walletService.findByAddress(address)
                .filter(w -> w.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Wallet access denied"));
    }
}