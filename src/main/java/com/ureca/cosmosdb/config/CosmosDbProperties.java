package com.ureca.cosmosdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cosmos")
public class CosmosDbProperties {
    private String endpoint;
    private String key;
    private DefaultConfig defaultConfig = new DefaultConfig();
    private Connection connection = new Connection();

    public String getEndpoint() {
        return endpoint;
    }

    public String getKey() {
        return key;
    }

    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDefaultConfig(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public static class DefaultConfig {
        private String database;

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }
    }

    public static class Connection {
        private String mode = "DIRECT";
        private int maxConnections = 100;
        private String requestTimeout = "PT60S";

        public String getMode() {
            return mode;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public String getRequestTimeout() {
            return requestTimeout;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public void setRequestTimeout(String requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }
}
