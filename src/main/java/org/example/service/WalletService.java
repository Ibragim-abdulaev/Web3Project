package org.example.service;

import org.example.entity.User;
import org.example.entity.Wallet;
import org.example.exception.WalletCreationException;
import org.example.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    @Qualifier("ethereumWeb3j")
    private Web3j ethereumWeb3j;

    @Autowired
    @Qualifier("polygonWeb3j")
    private Web3j polygonWeb3j;

    @Cacheable(value = "wallets", key = "#address")
    public Optional<Wallet> findByAddress(String address) {
        logger.debug("Finding wallet by address: {}", address);
        return walletRepository.findByAddress(address);
    }

    @Cacheable(value = "wallets", key = "'user_' + #userId")
    public List<Wallet> findByUserId(Long userId) {
        logger.debug("Finding wallets for user: {}", userId);
        return walletRepository.findByUserId(userId);
    }

    public List<Wallet> findActiveWalletsByUser(Long userId) {
        return walletRepository.findByUserIdAndIsActive(userId, true);
    }

    public Wallet createWallet(User user, Wallet.NetworkType network) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        logger.info("Creating new wallet for user: {} on network: {}", user.getUsername(), network);

        String privateKey = generatePrivateKey();
        Credentials credentials = Credentials.create(privateKey);
        String address = credentials.getAddress();

        if (walletRepository.existsByAddress(address)) {
            logger.warn("Wallet creation failed — address already exists: {}", address);
            throw new WalletCreationException("Wallet with this address already exists");
        }

        Wallet wallet = new Wallet();
        wallet.setAddress(address);
        wallet.setPrivateKey(privateKey);
        wallet.setNetwork(network);
        wallet.setUser(user);
        wallet.setIsActive(true);
        wallet.setBalance(BigDecimal.ZERO);

        Wallet savedWallet = walletRepository.save(wallet);
        logger.info("Wallet created successfully: {}", address);

        clearUserWalletsCache(user.getId());
        return savedWallet;
    }

    @Cacheable(value = "wallets", key = "'balance_' + #address")
    public BigDecimal getBalance(String address, Wallet.NetworkType network) {
        logger.debug("Getting balance for address: {} on network: {}", address, network);

        try {
            Web3j web3j = getWeb3jByNetwork(network);
            EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            BigInteger balance = ethGetBalance.getBalance();

            BigDecimal balanceInEther = Convert.fromWei(new BigDecimal(balance), Convert.Unit.ETHER);

            Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                wallet.setBalance(balanceInEther);
                walletRepository.save(wallet);
            }

            return balanceInEther;

        } catch (Exception e) {
            logger.error("Error getting balance for address: {}", address, e);
            return BigDecimal.ZERO;
        }
    }

    public void updateBalance(String address, BigDecimal newBalance) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);

            clearBalanceCache(address);
        }
    }

    public boolean isValidAddress(String address) {
        try {
            return WalletUtils.isValidAddress(address);
        } catch (Exception e) {
            return false;
        }
    }

    public List<Wallet> findWalletsByNetwork(Wallet.NetworkType network) {
        return walletRepository.findByNetworkAndIsActive(network, true);
    }

    @CacheEvict(value = "wallets", key = "'user_' + #userId")
    public void clearUserWalletsCache(Long userId) {
        logger.debug("Clearing wallet cache for user: {}", userId);
    }

    @CacheEvict(value = "wallets", key = "'balance_' + #address")
    public void clearBalanceCache(String address) {
        logger.debug("Clearing balance cache for address: {}", address);
    }

    public void deactivateWallet(String address) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.setIsActive(false);
            walletRepository.save(wallet);

            clearUserWalletsCache(wallet.getUser().getId());
            clearBalanceCache(address);
        }
    }

    public Long getWalletCountByNetwork(Wallet.NetworkType network) {
        return walletRepository.countByNetwork(network);
    }

    private String generatePrivateKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        return Keys.createEcKeyPair().getPrivateKey().toString(16);
    }

    private Web3j getWeb3jByNetwork(Wallet.NetworkType network) {
        return switch (network) {
            case ETHEREUM -> ethereumWeb3j;
            case POLYGON -> polygonWeb3j;
            default -> ethereumWeb3j;
        };
    }
}