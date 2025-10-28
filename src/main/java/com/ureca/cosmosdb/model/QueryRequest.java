package com.ureca.cosmosdb.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Query request containing SQL query and parameters")
public class QueryRequest {
    
    @Schema(description = "SQL query with parameterized syntax (e.g., SELECT * FROM c WHERE c.userId = @userId)", 
            example = "SELECT * FROM c WHERE c.userId = @userId", 
            required = true)
    private String sql;
    
    @Schema(description = "Map of parameter names to values for parameterized query", 
            example = "{\"userId\": \"u-001\"}")
    private Map<String, Object> params;
}
