package com.ureca.cosmosdb.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cosmos DB metadata and diagnostics")
public class CosmosMetadata {
    
    @Schema(description = "Request Units (RU) consumed by this operation", example = "2.83")
    private double ru;
    
    @Schema(description = "HTTP status code from Cosmos DB", example = "200")
    private int statusCode;
    
    @Schema(description = "Cosmos DB activity ID for tracing and diagnostics", example = "12345-abcde")
    private String activityId;
    
    @Schema(description = "Cosmos DB sub-status code for detailed error information", example = "0")
    private int subStatus;
    
    @Schema(description = "Retry after duration in milliseconds (present on 429 throttling)", example = "5000")
    private Integer retryAfterMs;
}
