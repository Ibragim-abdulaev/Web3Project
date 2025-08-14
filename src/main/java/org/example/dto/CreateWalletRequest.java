package org.example.dto;

import jakarta.validation.constraints.NotNull;
import org.example.entity.Wallet;

public class CreateWalletRequest {

    @NotNull(message = "Network type is required")
    private Wallet.NetworkType network;

    public Wallet.NetworkType getNetwork() { return network; }
    public void setNetwork(Wallet.NetworkType network) { this.network = network; }
}
