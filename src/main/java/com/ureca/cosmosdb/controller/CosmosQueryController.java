package com.ureca.cosmosdb.controller;

import com.ureca.cosmosdb.model.QueryRequest;
import com.ureca.cosmosdb.model.QueryResponse;
import com.ureca.cosmosdb.service.CosmosQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cosmos/v1")
@RequiredArgsConstructor
@Slf4j
public class CosmosQueryController {

    private final CosmosQueryService queryService;

    @PostMapping("/query/{container}")
    public ResponseEntity<QueryResponse> query(
            @PathVariable String container,
            @RequestBody QueryRequest request,
            @RequestParam(required = false) String pk,
            @RequestParam(required = false) Integer maxItemCount,
            @RequestParam(required = false) String ct,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
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
