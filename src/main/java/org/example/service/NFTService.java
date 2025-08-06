package org.example.service;

import org.example.entity.NFT;
import org.example.entity.Wallet;
import org.example.repository.NFTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NFTService {

    @Autowired
    private NFTRepository nftRepository;

    @Cacheable(value = "nfts", key = "#contractAddress + '_' + #tokenId")
    public Optional<NFT> getNFTByTokenIdAndContract(String tokenId, String contractAddress) {
        return nftRepository.findByTokenIdAndContractAddress(tokenId, contractAddress);
    }

    public Page<NFT> getNFTsByOwner(String ownerAddress, Pageable pageable) {
        return nftRepository.findByOwnerAddressOrderByCreatedAtDesc(ownerAddress, pageable);
    }

    public List<NFT> getNFTsByContract(String contractAddress) {
        return nftRepository.findByContractAddress(contractAddress);
    }

    public Page<NFT> searchNFTs(String keyword, Pageable pageable) {
        return nftRepository.searchByKeyword(keyword, pageable);
    }

    public List<NFT> getPopularNFTsByNetwork(Wallet.NetworkType network, Pageable pageable) {
        return nftRepository.findPopularNFTsByNetwork(network, pageable);
    }

    public List<String> getAllCollections() {
        return nftRepository.findAllCollections();
    }

    public Long getNFTCountByOwner(String ownerAddress) {
        return nftRepository.countByOwnerAddress(ownerAddress);
    }

    public List<NFT> getLatestNFTsByOwner(String ownerAddress, int limit) {
        return nftRepository.findLatestNFTsByOwner(ownerAddress, limit);
    }
}