package com.ureca.cosmosdb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmosMetadata {
    private double ru;
    private int statusCode;
    private String activityId;
    private int subStatus;
    private Integer retryAfterMs;
}
