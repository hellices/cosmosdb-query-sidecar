package com.ureca.cosmosdb.service;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.ureca.cosmosdb.config.CosmosDbProperties;
import com.ureca.cosmosdb.model.CosmosMetadata;
import com.ureca.cosmosdb.model.ErrorInfo;
import com.ureca.cosmosdb.model.QueryData;
import com.ureca.cosmosdb.model.QueryRequest;
import com.ureca.cosmosdb.model.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CosmosQueryService {

    private final CosmosAsyncClient cosmosAsyncClient;
    private final CosmosDbProperties properties;

    public Mono<QueryResponse> executeQuery(String containerName, QueryRequest request, 
                                     String partitionKey, Integer maxItemCount, 
                                     String continuationToken) {
        try {
            log.debug("Executing query on container: {}, partition key: {}", containerName, partitionKey);
            
            CosmosAsyncDatabase database = cosmosAsyncClient.getDatabase(properties.getDefaultConfig().getDatabase());
            CosmosAsyncContainer container = database.getContainer(containerName);

            // Build SQL query spec with parameters
            SqlQuerySpec querySpec = buildQuerySpec(request);

            // Configure query options
            CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
            if (partitionKey != null) {
                options.setPartitionKey(new PartitionKey(partitionKey));
                log.debug("Using partition key: {}", partitionKey);
            }
            if (maxItemCount != null && maxItemCount > 0) {
                options.setMaxDegreeOfParallelism(maxItemCount);
            }

            // Execute query reactively
            return container.queryItems(querySpec, options, Object.class)
                    .byPage(continuationToken, maxItemCount)
                    .next() // Get the first page
                    .map(this::buildSuccessResponse)
                    .onErrorResume(CosmosException.class, this::buildErrorResponseMono)
                    .onErrorResume(Exception.class, this::buildGenericErrorResponseMono);

        } catch (Exception e) {
            log.error("Unexpected error executing query", e);
            return buildGenericErrorResponseMono(e);
        }
    }

    private QueryResponse buildSuccessResponse(FeedResponse<Object> feedResponse) {
        List<Object> results = new ArrayList<>();
        feedResponse.getResults().forEach(results::add);

        // Extract diagnostics
        double requestCharge = feedResponse.getRequestCharge();
        String activityId = feedResponse.getActivityId();
        String newContinuationToken = feedResponse.getContinuationToken();

        log.info("Query executed successfully. Results: {}, RU: {}, ActivityId: {}", 
                results.size(), requestCharge, activityId);

        // Build response
        QueryData data = QueryData.builder()
                .count(results.size())
                .results(results)
                .continuationToken(newContinuationToken)
                .build();

        CosmosMetadata metadata = CosmosMetadata.builder()
                .ru(requestCharge)
                .statusCode(200)
                .activityId(activityId)
                .subStatus(0)
                .build();

        return QueryResponse.builder()
                .ok(true)
                .data(data)
                .cosmos(metadata)
                .build();
    }

    private SqlQuerySpec buildQuerySpec(QueryRequest request) {
        List<SqlParameter> parameters = new ArrayList<>();
        
        if (request.getParams() != null && !request.getParams().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getParams().entrySet()) {
                // Add @ prefix if not present
                String paramName = entry.getKey().startsWith("@") ? entry.getKey() : "@" + entry.getKey();
                parameters.add(new SqlParameter(paramName, entry.getValue()));
            }
        }

        return new SqlQuerySpec(request.getSql(), parameters);
    }

    private Mono<QueryResponse> buildErrorResponseMono(CosmosException e) {
        log.error("Cosmos DB error: {}, Status: {}, SubStatus: {}, ActivityId: {}", 
                e.getMessage(), e.getStatusCode(), e.getSubStatusCode(), e.getActivityId());
        return Mono.just(buildErrorResponse(e));
    }

    private QueryResponse buildErrorResponse(CosmosException e) {
        String errorCode = determineErrorCode(e.getStatusCode());
        
        Map<String, Object> details = new HashMap<>();
        details.put("activityId", e.getActivityId());
        details.put("subStatus", e.getSubStatusCode());
        if (e.getRetryAfterDuration() != null) {
            details.put("retryAfterMs", e.getRetryAfterDuration().toMillis());
        }

        ErrorInfo errorInfo = ErrorInfo.builder()
                .code(errorCode)
                .message(e.getMessage())
                .details(details)
                .build();

        CosmosMetadata.CosmosMetadataBuilder metadataBuilder = CosmosMetadata.builder()
                .ru(e.getRequestCharge())
                .statusCode(e.getStatusCode())
                .activityId(e.getActivityId())
                .subStatus(e.getSubStatusCode());
        
        if (e.getRetryAfterDuration() != null) {
            metadataBuilder.retryAfterMs((int) e.getRetryAfterDuration().toMillis());
        }

        return QueryResponse.builder()
                .ok(false)
                .error(errorInfo)
                .cosmos(metadataBuilder.build())
                .build();
    }

    private Mono<QueryResponse> buildGenericErrorResponseMono(Exception e) {
        log.error("Unexpected error executing query", e);
        return Mono.just(buildGenericErrorResponse(e));
    }

    private QueryResponse buildGenericErrorResponse(Exception e) {
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("UpstreamError")
                .message(e.getMessage() != null ? e.getMessage() : "Internal server error")
                .build();

        CosmosMetadata metadata = CosmosMetadata.builder()
                .ru(0.0)
                .statusCode(500)
                .activityId("N/A")
                .subStatus(0)
                .build();

        return QueryResponse.builder()
                .ok(false)
                .error(errorInfo)
                .cosmos(metadata)
                .build();
    }

    private String determineErrorCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> "BadRequest";
            case 404 -> "NotFound";
            case 429 -> "Throttled";
            case 408 -> "Timeout";
            default -> "UpstreamError";
        };
    }
}
