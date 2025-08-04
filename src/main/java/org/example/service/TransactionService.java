package org.example.service;

import org.example.entity.Transaction;
import org.example.entity.Wallet;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    @Qualifier("ethereumWeb3j")
    private Web3j ethereumWeb3j;

    @Autowired
    @Qualifier("polygonWeb3j")
    private Web3j polygonWeb3j;

    @Autowired
    @Qualifier("ethereumCredentials")
    private Credentials ethereumCredentials;

    @Autowired
    @Qualifier("polygonCredentials")
    private Credentials polygonCredentials;

    @Autowired
    private ContractGasProvider gasProvider;

    @Cacheable(value = "transactions", key = "#hash")
    public Optional<Transaction> findByHash(String hash) {
        logger.debug("Finding transaction by hash: {}", hash);
        return transactionRepository.findByHash(hash);
    }

    public Page<Transaction> getTransactionsByAddress(String address, Pageable pageable) {
        logger.debug("Getting transactions for address: {}", address);
        return transactionRepository.findByAddressOrderByCreatedAtDesc(address, pageable);
    }

    public List<Transaction> getTransactionsByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public CompletableFuture<String> sendTransaction(String fromAddress, String toAddress,
                                                     BigDecimal amount, Wallet.NetworkType network,
                                                     String privateKey) {
        logger.info("Sending transaction from {} to {} amount: {} on network: {}",
                fromAddress, toAddress, amount, network);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!walletService.isValidAddress(fromAddress) || !walletService.isValidAddress(toAddress)) {
                    throw new RuntimeException("Invalid wallet address");
                }

                BigDecimal balance = walletService.getBalance(fromAddress, network);
                if (balance.compareTo(amount) < 0) {
                    throw new RuntimeException("Insufficient balance");
                }

                Web3j web3j = getWeb3jByNetwork(network);
                Credentials credentials = Credentials.create(privateKey);

                TransactionReceipt receipt = Transfer.sendFunds(
                        web3j,
                        credentials,
                        toAddress,
                        amount,
                        Convert.Unit.ETHER
                ).send();

                Transaction transaction = new Transaction();
                transaction.setHash(receipt.getTransactionHash());
                transaction.setFromAddress(fromAddress);
                transaction.setToAddress(toAddress);
                transaction.setAmount(amount);
                transaction.setNetwork(network);
                transaction.setType(Transaction.TransactionType.TRANSFER);
                transaction.setStatus(receipt.isStatusOK() ?
                        Transaction.TransactionStatus.CONFIRMED : Transaction.TransactionStatus.FAILED);
                transaction.setBlockNumber(receipt.getBlockNumber().longValue());
                transaction.setBlockHash(receipt.getBlockHash());
                transaction.setGasUsed(new BigDecimal(receipt.getGasUsed()));
                transaction.setBlockchainTimestamp(LocalDateTime.now());

                Optional<Wallet> fromWallet = walletRepository.findByAddress(fromAddress);
                Optional<Wallet> toWallet = walletRepository.findByAddress(toAddress);
                fromWallet.ifPresent(transaction::setFromWallet);
                toWallet.ifPresent(transaction::setToWallet);

                transactionRepository.save(transaction);

                updateWalletBalances(fromAddress, toAddress, network);

                logger.info("Transaction sent successfully: {}", receipt.getTransactionHash());
                return receipt.getTransactionHash();

            } catch (Exception e) {
                logger.error("Error sending transaction", e);
                throw new RuntimeException("Failed to send transaction: " + e.getMessage());
            }
        });
    }

    @Async
    public void updateTransactionStatus(String hash) {
        logger.debug("Updating transaction status for hash: {}", hash);

        Optional<Transaction> transactionOpt = transactionRepository.findByHash(hash);
        if (!transactionOpt.isPresent()) {
            return;
        }

        Transaction transaction = transactionOpt.get();
        if (transaction.getStatus() == Transaction.TransactionStatus.CONFIRMED) {
            return; // Already confirmed
        }

        try {
            Web3j web3j = getWeb3jByNetwork(transaction.getNetwork());

            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(hash).send();

            if (receiptResponse.getTransactionReceipt().isPresent()) {
                TransactionReceipt receipt = receiptResponse.getTransactionReceipt().get();

                transaction.setStatus(receipt.isStatusOK() ?
                        Transaction.TransactionStatus.CONFIRMED : Transaction.TransactionStatus.FAILED);
                transaction.setBlockNumber(receipt.getBlockNumber().longValue());
                transaction.setBlockHash(receipt.getBlockHash());
                transaction.setGasUsed(new BigDecimal(receipt.getGasUsed()));

                EthBlockNumber latestBlock = web3j.ethBlockNumber().send();
                long confirmations = latestBlock.getBlockNumber().longValue() - receipt.getBlockNumber().longValue();
                transaction.setConfirmationCount((int) confirmations);

                transactionRepository.save(transaction);

                clearTransactionCache(hash);

                logger.info("Transaction status updated: {} - {}", hash, transaction.getStatus());
            }

        } catch (Exception e) {
            logger.error("Error updating transaction status for hash: {}", hash, e);
        }
    }

    public BigDecimal getTotalSentAmount(String address) {
        BigDecimal total = transactionRepository.getTotalSentAmount(address);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalReceivedAmount(String address) {
        BigDecimal total = transactionRepository.getTotalReceivedAmount(address);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Transaction> getLatestTransactionsByNetwork(Wallet.NetworkType network, int limit) {
        return transactionRepository.findLatestTransactionsByNetwork(network.name(), limit);
    }

    public Long getTransactionCountByStatus(Transaction.TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    @CacheEvict(value = "transactions", key = "#hash")
    public void clearTransactionCache(String hash) {
        logger.debug("Clearing transaction cache for hash: {}", hash);
    }

    private void updateWalletBalances(String fromAddress, String toAddress, Wallet.NetworkType network) {
        // Update sender balance
        BigDecimal fromBalance = walletService.getBalance(fromAddress, network);
        walletService.updateBalance(fromAddress, fromBalance);

        // Update receiver balance
        BigDecimal toBalance = walletService.getBalance(toAddress, network);
        walletService.updateBalance(toAddress, toBalance);
    }

    private Web3j getWeb3jByNetwork(Wallet.NetworkType network) {
        return switch (network) {
            case ETHEREUM -> ethereumWeb3j;
            case POLYGON -> polygonWeb3j;
            default -> ethereumWeb3j;
        };
    }

    private Credentials getCredentialsByNetwork(Wallet.NetworkType network) {
        return switch (network) {
            case ETHEREUM -> ethereumCredentials;
            case POLYGON -> polygonCredentials;
            default -> ethereumCredentials;
        };
    }

    public void cleanupOldPendingTransactions() {
        logger.info("Cleaning up old pending transactions...");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<Transaction> oldPendingTxs = transactionRepository.findPendingTransactionsOlderThan(
                Transaction.TransactionStatus.PENDING, cutoff);

        for (Transaction tx : oldPendingTxs) {
            updateTransactionStatus(tx.getHash());
        }

        logger.info("Cleaned up {} old pending transactions", oldPendingTxs.size());
    }
}