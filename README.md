# Cosmos DB Query Sidecar

A lightweight Spring Boot sidecar service that provides a single HTTP endpoint for executing Cosmos DB queries using Azure Cosmos DB Java SDK v4 in Direct mode.

## Features

- ✅ Single endpoint: `POST /cosmos/v1/query/{container}`
- ✅ Direct mode connection for reduced latency
- ✅ Partition key support for optimized queries
- ✅ Request Unit (RU) tracking and diagnostics
- ✅ Continuation token support for paging
- ✅ Standardized JSON response format
- ✅ Docker containerization ready

## Quick Start

### Prerequisites

- Java 17+
- Gradle 8.5+ (or use the wrapper)
- Azure Cosmos DB account

### Configuration

Set the following environment variables:

```bash
export COSMOS_ENDPOINT=https://your-account.documents.azure.com:443/
export COSMOS_KEY=your-primary-key
export COSMOS_DEFAULT_DB=ureca_evo
```

### Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The service will start on port 8080.

### Docker

```bash
# Build image
docker build -t cosmosdb-query-sidecar:1.0.0 .

# Run container
docker run -p 8080:8080 \
  -e COSMOS_ENDPOINT=https://your-account.documents.azure.com:443/ \
  -e COSMOS_KEY=your-primary-key \
  -e COSMOS_DEFAULT_DB=ureca_evo \
  cosmosdb-query-sidecar:1.0.0
```

## API Usage

### Endpoint

```
POST /cosmos/v1/query/{container}
```

### Request Body

```json
{
  "sql": "SELECT * FROM c WHERE c.userId = @userId",
  "params": { "userId": "u-001" }
}
```

### Query Parameters (Optional)

- `pk` - Partition key value (recommended for best performance)
- `maxItemCount` - Page size (number of items per page)
- `ct` - Continuation token for pagination

### Request Headers (Optional)

- `X-Request-Id` - Correlation ID for tracing
- `X-Timeout-Ms` - Request timeout in milliseconds

### Example Request

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/users \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: req-123" \
  -d '{
    "sql": "SELECT * FROM c WHERE c.userId = @userId",
    "params": { "userId": "u-001" }
  }' \
  "?pk=u-001&maxItemCount=10"
```

### Success Response (200 OK)

```json
{
  "ok": true,
  "data": {
    "count": 2,
    "results": [
      { "id": "1", "userId": "u-001", "name": "John" },
      { "id": "2", "userId": "u-001", "name": "Jane" }
    ],
    "continuationToken": null
  },
  "cosmos": {
    "ru": 2.83,
    "statusCode": 200,
    "activityId": "12345-abcde",
    "subStatus": 0
  }
}
```

### Error Response (429 Too Many Requests)

```json
{
  "ok": false,
  "error": {
    "code": "Throttled",
    "message": "Request rate is large",
    "details": {
      "activityId": "12345-abcde",
      "subStatus": 3200,
      "retryAfterMs": 5000
    }
  },
  "cosmos": {
    "ru": 0.0,
    "statusCode": 429,
    "activityId": "12345-abcde",
    "subStatus": 3200,
    "retryAfterMs": 5000
  }
}
```

### Response Headers

- `X-Cosmos-RU` - Request units consumed
- `X-Cosmos-Activity-Id` - Cosmos DB activity ID for tracing
- `X-Cosmos-SubStatus` - Cosmos DB sub-status code
- `X-Cosmos-Retry-After-Ms` - Retry after duration (on 429)
- `X-Request-Id` - Echoed request ID

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

## Connection Mode

The service uses **Direct mode** by default for optimal performance:

- Lower latency (bypasses gateway)
- Direct connection to Cosmos DB data nodes
- Reduced network hops

Connection mode can be configured via `cosmos.connection.mode` property (DIRECT or GATEWAY).

## Performance Tips

1. **Always provide partition key** (`pk` parameter) when possible for best performance
2. **Use continuation tokens** for large result sets instead of fetching all at once
3. **Monitor RU consumption** via response headers and adjust queries accordingly
4. **Direct mode** provides better latency than Gateway mode

## Deployment Runbook

### 1. Build the Container

```bash
docker build -t cosmosdb-query-sidecar:1.0.0 .
```

### 2. Push to Registry

```bash
docker tag cosmosdb-query-sidecar:1.0.0 your-registry/cosmosdb-query-sidecar:1.0.0
docker push your-registry/cosmosdb-query-sidecar:1.0.0
```

### 3. Deploy to Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cosmosdb-query-sidecar
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cosmosdb-query-sidecar
  template:
    metadata:
      labels:
        app: cosmosdb-query-sidecar
    spec:
      containers:
      - name: sidecar
        image: your-registry/cosmosdb-query-sidecar:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: COSMOS_ENDPOINT
          valueFrom:
            secretKeyRef:
              name: cosmos-secrets
              key: endpoint
        - name: COSMOS_KEY
          valueFrom:
            secretKeyRef:
              name: cosmos-secrets
              key: key
        - name: COSMOS_DEFAULT_DB
          value: "ureca_evo"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: cosmosdb-query-sidecar
spec:
  selector:
    app: cosmosdb-query-sidecar
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

### 4. Create Secrets

```bash
kubectl create secret generic cosmos-secrets \
  --from-literal=endpoint=https://your-account.documents.azure.com:443/ \
  --from-literal=key=your-primary-key
```

### 5. Apply Deployment

```bash
kubectl apply -f deployment.yaml
```

### 6. Verify Deployment

```bash
kubectl get pods -l app=cosmosdb-query-sidecar
kubectl logs -l app=cosmosdb-query-sidecar
```

## Troubleshooting

### Connection Issues

- Verify `COSMOS_ENDPOINT` and `COSMOS_KEY` are correct
- Check network connectivity to Cosmos DB
- Review logs for connection errors

### High Latency

- Ensure Direct mode is enabled (default)
- Provide partition key in queries
- Monitor RU consumption and optimize queries

### 429 Throttling Errors

- Respect `retryAfterMs` in error responses
- Consider increasing Cosmos DB throughput (RU/s)
- Optimize queries to reduce RU consumption

## License

MIT
