package com.ureca.cosmosdb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cosmos DB Query Sidecar API")
                        .version("1.0.0")
                        .description("A lightweight Spring Boot sidecar service that provides a single HTTP endpoint " +
                                "for executing Cosmos DB queries using Azure Cosmos DB Java SDK v4 in Direct mode.")
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(String.format("http://localhost:%d", serverPort))
                                .description("Development server")
                ));
    }
}