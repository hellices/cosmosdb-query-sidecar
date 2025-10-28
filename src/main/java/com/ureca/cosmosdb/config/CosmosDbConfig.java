package com.ureca.cosmosdb.config;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.cosmos.GatewayConnectionConfig;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
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
    public CosmosAsyncClient cosmosAsyncClient() {
        log.info("Initializing Cosmos DB async client with endpoint: {}", properties.getEndpoint());
        log.info("Authentication mode: {}", properties.getAuth().getMode());
        log.info("Connection mode: {}", properties.getConnection().getMode());
        
        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(properties.getEndpoint());

        // Configure authentication based on mode
        String authMode = properties.getAuth().getMode();
        if ("DEFAULT_AZURE_CREDENTIAL".equalsIgnoreCase(authMode)) {
            // DefaultAzureCredential is created once since this bean is a singleton
            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            builder.credential(credential);
            log.info("Cosmos DB async client configured with DefaultAzureCredential");
        } else if ("KEY".equalsIgnoreCase(authMode)) {
            if (properties.getKey() == null || properties.getKey().isEmpty()) {
                throw new IllegalArgumentException("cosmos.key must be set when auth.mode is KEY");
            }
            builder.key(properties.getKey());
            log.info("Cosmos DB async client configured with Key-based authentication");
        } else {
            throw new IllegalArgumentException("Invalid auth.mode: " + authMode + ". Valid values are: KEY, DEFAULT_AZURE_CREDENTIAL");
        }

        // Configure Direct mode or Gateway mode
        if ("DIRECT".equalsIgnoreCase(properties.getConnection().getMode())) {
            DirectConnectionConfig directConfig = DirectConnectionConfig.getDefaultConfig();
            builder.directMode(directConfig);
            log.info("Cosmos DB async client configured with Direct mode");
        } else {
            GatewayConnectionConfig gatewayConfig = GatewayConnectionConfig.getDefaultConfig();
            builder.gatewayMode(gatewayConfig);
            log.info("Cosmos DB async client configured with Gateway mode");
        }

        return builder.buildAsyncClient();
    }
}
