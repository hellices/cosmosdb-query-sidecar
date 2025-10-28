package com.ureca.cosmosdb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {
    private boolean ok;
    private QueryData data;
    private ErrorInfo error;
    private CosmosMetadata cosmos;
}
