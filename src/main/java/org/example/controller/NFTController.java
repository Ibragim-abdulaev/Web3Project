package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.entity.NFT;
import org.example.entity.Wallet;
import org.example.service.NFTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/nft")
@Tag(name = "NFT Management", description = "Operations related to NFTs")
public class NFTController {

    @Autowired
    private NFTService nftService;

    @GetMapping("/owner/{address}")
    @Operation(summary = "Get NFTs by owner", description = "Get all NFTs owned by a specific address")
    public ResponseEntity<?> getNFTsByOwner(
            @PathVariable String address,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NFT> nfts = nftService.getNFTsByOwner(address, pageable);

        return ResponseEntity.ok(nfts);
    }

    @GetMapping("/contract/{contractAddress}")
    @Operation(summary = "Get NFTs by contract", description = "Get all NFTs from a specific contract")
    public ResponseEntity<?> getNFTsByContract(@PathVariable String contractAddress) {
        List<NFT> nfts = nftService.getNFTsByContract(contractAddress);
        return ResponseEntity.ok(Map.of("nfts", nfts, "count", nfts.size()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search NFTs", description = "Search NFTs by keyword")
    public ResponseEntity<?> searchNFTs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NFT> nfts = nftService.searchNFTs(keyword, pageable);

        return ResponseEntity.ok(nfts);
    }

    @GetMapping("/popular/{network}")
    @Operation(summary = "Get popular NFTs", description = "Get popular NFTs by network")
    public ResponseEntity<?> getPopularNFTs(
            @PathVariable Wallet.NetworkType network,
            @RequestParam(defaultValue = "10") int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<NFT> nfts = nftService.getPopularNFTsByNetwork(network, pageable);

        return ResponseEntity.ok(Map.of("nfts", nfts, "network", network));
    }

    @GetMapping("/collections")
    @Operation(summary = "Get all collections", description = "Get list of all NFT collections")
    public ResponseEntity<?> getAllCollections() {
        List<String> collections = nftService.getAllCollections();
        return ResponseEntity.ok(Map.of("collections", collections, "count", collections.size()));
    }

    @GetMapping("/{contractAddress}/{tokenId}")
    @Operation(summary = "Get NFT details", description = "Get detailed information about a specific NFT")
    public ResponseEntity<?> getNFTDetails(
            @PathVariable String contractAddress,
            @PathVariable String tokenId) {

        return nftService.getNFTByTokenIdAndContract(tokenId, contractAddress)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
