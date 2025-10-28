package com.ureca.cosmosdb.controller;

import com.ureca.cosmosdb.model.QueryRequest;
import com.ureca.cosmosdb.model.QueryResponse;
import com.ureca.cosmosdb.service.CosmosQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cosmos/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cosmos DB Query", description = "Cosmos DB query execution API")
public class CosmosQueryController {

    private final CosmosQueryService queryService;

    @PostMapping("/query/{container}")
    @Operation(
        summary = "Execute a Cosmos DB query",
        description = "Executes a SQL query against a specified Cosmos DB container with optional partition key and pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Query executed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid query syntax or parameters"),
        @ApiResponse(responseCode = "404", description = "Container not found"),
        @ApiResponse(responseCode = "408", description = "Request timeout"),
        @ApiResponse(responseCode = "429", description = "Too many requests - rate limited by Cosmos DB"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueryResponse> query(
            @Parameter(description = "Cosmos DB container name", required = true)
            @PathVariable String container,
            
            @Parameter(description = "Query request with SQL and parameters", required = true)
            @RequestBody QueryRequest request,
            
            @Parameter(description = "Partition key value for optimized query performance")
            @RequestParam(required = false) String pk,
            
            @Parameter(description = "Maximum number of items to return per page")
            @RequestParam(required = false) Integer maxItemCount,
            
            @Parameter(description = "Continuation token for pagination")
            @RequestParam(required = false) String ct,
            
            @Parameter(description = "Request ID for correlation and tracing")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            
            @Parameter(description = "Request timeout in milliseconds")
            @RequestHeader(value = "X-Timeout-Ms", required = false) Integer timeoutMs) {

        log.info("Received query request for container: {}, requestId: {}", container, requestId);
        log.debug("Query SQL: {}, Params: {}, PK: {}", request.getSql(), request.getParams(), pk);

        QueryResponse response = queryService.executeQuery(container, request, pk, maxItemCount, ct);

        // Build response with headers
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(determineHttpStatus(response));
        
        // Add standard headers
        if (response.getCosmos() != null) {
            responseBuilder.header("X-Cosmos-RU", String.valueOf(response.getCosmos().getRu()));
            responseBuilder.header("X-Cosmos-Activity-Id", response.getCosmos().getActivityId());
            responseBuilder.header("X-Cosmos-SubStatus", String.valueOf(response.getCosmos().getSubStatus()));
            
            if (response.getCosmos().getRetryAfterMs() != null) {
                responseBuilder.header("X-Cosmos-Retry-After-Ms", 
                        String.valueOf(response.getCosmos().getRetryAfterMs()));
            }
        }
        
        if (requestId != null) {
            responseBuilder.header("X-Request-Id", requestId);
        }

        return responseBuilder.body(response);
    }

    private HttpStatus determineHttpStatus(QueryResponse response) {
        if (response.isOk()) {
            return HttpStatus.OK;
        }

        if (response.getCosmos() != null) {
            int statusCode = response.getCosmos().getStatusCode();
            return switch (statusCode) {
                case 400 -> HttpStatus.BAD_REQUEST;
                case 404 -> HttpStatus.NOT_FOUND;
                case 429 -> HttpStatus.TOO_MANY_REQUESTS;
                case 408 -> HttpStatus.REQUEST_TIMEOUT;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
