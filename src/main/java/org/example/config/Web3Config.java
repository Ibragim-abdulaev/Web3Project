package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

@Configuration
public class Web3Config {

    @Value("${web3.ethereum.node-url}")
    private String ethereumNodeUrl;

    @Value("${web3.ethereum.private-key}")
    private String ethereumPrivateKey;

    @Value("${web3.polygon.node-url}")
    private String polygonNodeUrl;

    @Value("${web3.polygon.private-key}")
    private String polygonPrivateKey;

    @Value("${web3.ethereum.gas-price}")
    private BigInteger gasPrice;

    @Value("${web3.ethereum.gas-limit}")
    private BigInteger gasLimit;

    @Bean(name = "ethereumWeb3j")
    public Web3j ethereumWeb3j() {
        return Web3j.build(new HttpService(ethereumNodeUrl));
    }

    @Bean(name = "polygonWeb3j")
    public Web3j polygonWeb3j() {
        return Web3j.build(new HttpService(polygonNodeUrl));
    }

    @Bean(name = "ethereumCredentials")
    public Credentials ethereumCredentials() {
        return Credentials.create(ethereumPrivateKey);
    }

    @Bean(name = "polygonCredentials")
    public Credentials polygonCredentials() {
        return Credentials.create(polygonPrivateKey);
    }

    @Bean
    public ContractGasProvider gasProvider() {
        return new DefaultGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                return gasPrice;
            }

            @Override
            public BigInteger getGasPrice() {
                return gasPrice;
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                return gasLimit;
            }

            @Override
            public BigInteger getGasLimit() {
                return gasLimit;
            }
        };
    }
}
