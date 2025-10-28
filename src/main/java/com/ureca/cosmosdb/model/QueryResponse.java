package com.ureca.cosmosdb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response from Cosmos DB query execution")
public class QueryResponse {
    
    @Schema(description = "Indicates if the query was successful", example = "true")
    private boolean ok;
    
    @Schema(description = "Query result data (present when ok=true)")
    private QueryData data;
    
    @Schema(description = "Error information (present when ok=false)")
    private ErrorInfo error;
    
    @Schema(description = "Cosmos DB metadata including RU consumption and diagnostics")
    private CosmosMetadata cosmos;
}
