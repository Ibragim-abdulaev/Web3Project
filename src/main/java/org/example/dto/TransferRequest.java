package org.example.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.entity.Wallet;

import java.math.BigDecimal;

public class TransferRequest {

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
