package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableJpaRepositories
@EnableAsync
@EnableTransactionManagement
public class Web3BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Web3BackendApplication.class, args);
        System.out.println("🚀 Web3 Backend API started successfully!");
        System.out.println("📖 API Documentation: http://localhost:8080/api/v1/swagger-ui.html");
        System.out.println("🔗 Health Check: http://localhost:8080/api/v1/actuator/health");
    }
}