package com.ureca.cosmosdb.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.cosmos.GatewayConnectionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CosmosDbConfig {

    private final CosmosDbProperties properties;

    @Bean
    public CosmosClient cosmosClient() {
        log.info("Initializing Cosmos DB client with endpoint: {}", properties.getEndpoint());
        log.info("Connection mode: {}", properties.getConnection().getMode());
        
        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(properties.getEndpoint())
                .key(properties.getKey());

        // Configure Direct mode or Gateway mode
        if ("DIRECT".equalsIgnoreCase(properties.getConnection().getMode())) {
            DirectConnectionConfig directConfig = DirectConnectionConfig.getDefaultConfig();
            builder.directMode(directConfig);
            log.info("Cosmos DB client configured with Direct mode");
        } else {
            GatewayConnectionConfig gatewayConfig = GatewayConnectionConfig.getDefaultConfig();
            builder.gatewayMode(gatewayConfig);
            log.info("Cosmos DB client configured with Gateway mode");
        }

        return builder.buildClient();
    }
}
