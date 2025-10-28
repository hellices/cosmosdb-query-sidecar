package com.ureca.cosmosdb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Query result data")
public class QueryData {
    
    @Schema(description = "Number of items returned in this response", example = "2")
    private int count;
    
    @Schema(description = "List of result items from the query")
    private List<Object> results;
    
    @Schema(description = "Continuation token for pagination (use in 'ct' query parameter for next page)")
    private String continuationToken;
}
