package com.ureca.cosmosdb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error information when query fails")
public class ErrorInfo {
    
    @Schema(description = "Error code", example = "Throttled")
    private String code;
    
    @Schema(description = "Error message", example = "Request rate is large")
    private String message;
    
    @Schema(description = "Additional error details")
    private Map<String, Object> details;
}
