package org.example.dto;

import org.example.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {

    private String address;
    private Wallet.NetworkType network;
    private BigDecimal balance;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public WalletResponse(String address, Wallet.NetworkType network, BigDecimal balance,
                          Boolean isActive, LocalDateTime createdAt) {
        this.address = address;
        this.network = network;
        this.balance = balance;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public String getAddress() { return address; }
    public Wallet.NetworkType getNetwork() { return network; }
    public BigDecimal getBalance() { return balance; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
