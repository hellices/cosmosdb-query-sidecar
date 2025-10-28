# Cosmos DB Query Sidecar - Examples

This document provides detailed examples of using the Cosmos DB Query Sidecar API.

## Table of Contents

1. [Basic Query](#basic-query)
2. [Query with Parameters](#query-with-parameters)
3. [Query with Partition Key](#query-with-partition-key)
4. [Pagination](#pagination)
5. [Error Handling](#error-handling)
6. [Performance Optimization](#performance-optimization)
7. [Authentication Configuration](#authentication-configuration)
8. [Migration from Direct Cosmos DB SDK](#migration-from-direct-cosmos-db-sdk)
9. [Monitoring & Observability](#monitoring--observability)

---

## Basic Query

### Simple SELECT query

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/users \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c",
    "params": {}
  }'
```

**Response:**
```json
{
  "ok": true,
  "data": {
    "count": 100,
    "results": [
      {"id": "1", "name": "Alice", "email": "alice@example.com"},
      {"id": "2", "name": "Bob", "email": "bob@example.com"}
    ],
    "continuationToken": "token123"
  },
  "cosmos": {
    "ru": 2.83,
    "statusCode": 200,
    "activityId": "abc-123",
    "subStatus": 0
  }
}
```

---

## Query with Parameters

### Using Named Parameters (@param)

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/users \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: req-001" \
  -d '{
    "sql": "SELECT * FROM c WHERE c.userId = @userId AND c.status = @status",
    "params": {
      "userId": "u-001",
      "status": "active"
    }
  }'
```

### Multiple Conditions

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/orders \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c WHERE c.customerId = @customerId AND c.total > @minTotal ORDER BY c.createdAt DESC",
    "params": {
      "customerId": "c-123",
      "minTotal": 100.0
    }
  }'
```

### IN Clause with Array Parameter

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/products \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c WHERE ARRAY_CONTAINS(@categories, c.category)",
    "params": {
      "categories": ["electronics", "computers", "phones"]
    }
  }'
```

---

## Query with Partition Key

### Optimized Query with Partition Key

Using the `pk` query parameter provides the best performance and lowest RU cost:

```bash
curl -X POST "http://localhost:8080/cosmos/v1/query/users?pk=u-001" \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c WHERE c.userId = @userId",
    "params": {
      "userId": "u-001"
    }
  }'
```

**Benefits:**
- Lower latency
- Reduced RU consumption
- No cross-partition query

### Comparison

**Without partition key:**
```json
{
  "cosmos": {
    "ru": 23.45
  }
}
```

**With partition key:**
```json
{
  "cosmos": {
    "ru": 2.83
  }
}
```

---

## Pagination

### First Page (with maxItemCount)

```bash
curl -X POST "http://localhost:8080/cosmos/v1/query/users?maxItemCount=10" \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c ORDER BY c.createdAt DESC",
    "params": {}
  }'
```

**Response:**
```json
{
  "ok": true,
  "data": {
    "count": 10,
    "results": [...],
    "continuationToken": "continuation-token-here"
  }
}
```

### Next Page (with continuation token)

```bash
curl -X POST "http://localhost:8080/cosmos/v1/query/users?maxItemCount=10&ct=continuation-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c ORDER BY c.createdAt DESC",
    "params": {}
  }'
```

### Complete Pagination Example (Node.js)

```javascript
async function getAllUsers(baseUrl, containerName) {
  const allResults = [];
  let continuationToken = null;

  do {
    const url = new URL(`${baseUrl}/cosmos/v1/query/${containerName}`);
    if (continuationToken) {
      url.searchParams.set('ct', continuationToken);
    }
    url.searchParams.set('maxItemCount', '100');

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sql: 'SELECT * FROM c',
        params: {}
      })
    });

    const result = await response.json();
    
    if (result.ok) {
      allResults.push(...result.data.results);
      continuationToken = result.data.continuationToken;
    } else {
      throw new Error(result.error.message);
    }
  } while (continuationToken);

  return allResults;
}
```

---

## Error Handling

### 404 Not Found (Container doesn't exist)

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/nonexistent \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM c",
    "params": {}
  }'
```

**Response (404):**
```json
{
  "ok": false,
  "error": {
    "code": "NotFound",
    "message": "Resource Not Found",
    "details": {
      "activityId": "xyz-789",
      "subStatus": 0
    }
  },
  "cosmos": {
    "ru": 0.0,
    "statusCode": 404,
    "activityId": "xyz-789",
    "subStatus": 0
  }
}
```

### 429 Too Many Requests (Throttled)

**Response (429):**
```json
{
  "ok": false,
  "error": {
    "code": "Throttled",
    "message": "Request rate is large",
    "details": {
      "activityId": "abc-456",
      "subStatus": 3200,
      "retryAfterMs": 5000
    }
  },
  "cosmos": {
    "ru": 0.0,
    "statusCode": 429,
    "activityId": "abc-456",
    "subStatus": 3200,
    "retryAfterMs": 5000
  }
}
```

**Response Headers:**
```
HTTP/1.1 429 Too Many Requests
X-Cosmos-RU: 0.0
X-Cosmos-Activity-Id: abc-456
X-Cosmos-SubStatus: 3200
X-Cosmos-Retry-After-Ms: 5000
```

### Error Handling Example (Node.js)

```javascript
async function queryWithRetry(url, requestBody, maxRetries = 3) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    });

    const result = await response.json();

    if (result.ok) {
      return result.data;
    }

    if (result.error.code === 'Throttled') {
      const retryAfter = result.error.details.retryAfterMs || 1000;
      console.log(`Throttled. Retrying after ${retryAfter}ms...`);
      await sleep(retryAfter);
      continue;
    }

    throw new Error(`${result.error.code}: ${result.error.message}`);
  }

  throw new Error('Max retries exceeded');
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
```

---

## Performance Optimization

### 1. Always Use Partition Keys

❌ **Bad - Cross-partition query:**
```bash
curl -X POST http://localhost:8080/cosmos/v1/query/users \
  -d '{"sql": "SELECT * FROM c WHERE c.email = @email", "params": {"email": "user@example.com"}}'
```
RU Cost: ~20-30 RU

✅ **Good - Partition-scoped query:**
```bash
curl -X POST "http://localhost:8080/cosmos/v1/query/users?pk=u-001" \
  -d '{"sql": "SELECT * FROM c WHERE c.userId = @userId", "params": {"userId": "u-001"}}'
```
RU Cost: ~2-3 RU

### 2. Use Projection (SELECT specific fields)

❌ **Bad - Select all fields:**
```json
{
  "sql": "SELECT * FROM c WHERE c.userId = @userId",
  "params": {"userId": "u-001"}
}
```

✅ **Good - Select only needed fields:**
```json
{
  "sql": "SELECT c.id, c.name, c.email FROM c WHERE c.userId = @userId",
  "params": {"userId": "u-001"}
}
```

### 3. Use Pagination for Large Results

❌ **Bad - Fetch all at once:**
```bash
curl -X POST http://localhost:8080/cosmos/v1/query/users \
  -d '{"sql": "SELECT * FROM c", "params": {}}'
```

✅ **Good - Use pagination:**
```bash
curl -X POST "http://localhost:8080/cosmos/v1/query/users?maxItemCount=100" \
  -d '{"sql": "SELECT * FROM c", "params": {}}'
```

### 4. Monitor RU Consumption

Always check the `X-Cosmos-RU` header to monitor costs:

```javascript
const response = await fetch(url, { /* ... */ });
const ruCost = response.headers.get('X-Cosmos-RU');
console.log(`Query cost: ${ruCost} RU`);
```

### 5. Use Request IDs for Tracing

```bash
curl -X POST http://localhost:8080/cosmos/v1/query/users \
  -H "X-Request-Id: req-$(date +%s)" \
  -d '{"sql": "SELECT * FROM c", "params": {}}'
```

The request ID will be echoed back in the response header for correlation.

---

## Integration Examples

### Node.js Client

```javascript
class CosmosDbClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
  }

  async query(container, sql, params = {}, options = {}) {
    const url = new URL(`${this.baseUrl}/cosmos/v1/query/${container}`);
    
    if (options.partitionKey) {
      url.searchParams.set('pk', options.partitionKey);
    }
    if (options.maxItemCount) {
      url.searchParams.set('maxItemCount', options.maxItemCount);
    }
    if (options.continuationToken) {
      url.searchParams.set('ct', options.continuationToken);
    }

    const headers = {
      'Content-Type': 'application/json',
    };
    if (options.requestId) {
      headers['X-Request-Id'] = options.requestId;
    }

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({ sql, params })
    });

    const result = await response.json();
    
    if (!result.ok) {
      const error = new Error(result.error.message);
      error.code = result.error.code;
      error.cosmos = result.cosmos;
      throw error;
    }

    return {
      results: result.data.results,
      count: result.data.count,
      continuationToken: result.data.continuationToken,
      ru: result.cosmos.ru,
      activityId: result.cosmos.activityId
    };
  }
}

// Usage
const client = new CosmosDbClient('http://localhost:8080');

const { results, ru } = await client.query(
  'users',
  'SELECT * FROM c WHERE c.userId = @userId',
  { userId: 'u-001' },
  { partitionKey: 'u-001', maxItemCount: 10 }
);

console.log(`Found ${results.length} results (${ru} RU)`);
```

### Python Client

```python
import requests
from typing import Dict, List, Optional

class CosmosDbClient:
    def __init__(self, base_url: str):
        self.base_url = base_url
    
    def query(self, container: str, sql: str, params: Dict = None, 
              partition_key: Optional[str] = None,
              max_item_count: Optional[int] = None,
              continuation_token: Optional[str] = None) -> Dict:
        
        url = f"{self.base_url}/cosmos/v1/query/{container}"
        query_params = {}
        
        if partition_key:
            query_params['pk'] = partition_key
        if max_item_count:
            query_params['maxItemCount'] = max_item_count
        if continuation_token:
            query_params['ct'] = continuation_token
        
        response = requests.post(
            url,
            params=query_params,
            json={"sql": sql, "params": params or {}}
        )
        
        result = response.json()
        
        if not result['ok']:
            raise Exception(f"{result['error']['code']}: {result['error']['message']}")
        
        return result

# Usage
client = CosmosDbClient('http://localhost:8080')

result = client.query(
    'users',
    'SELECT * FROM c WHERE c.userId = @userId',
    params={'userId': 'u-001'},
    partition_key='u-001'
)

print(f"Found {result['data']['count']} results ({result['cosmos']['ru']} RU)")
```

---

## Authentication Configuration

The sidecar supports two authentication modes that can be configured via environment variables without code changes.

### Key-based Authentication (Default)

Most common for development and testing:

```bash
export COSMOS_ENDPOINT=https://your-account.documents.azure.com:443/
export COSMOS_AUTH_MODE=KEY
export COSMOS_KEY=your-primary-or-secondary-key
export COSMOS_DEFAULT_DB=ureca_evo

# Start the sidecar
./gradlew bootRun
```

**Docker:**
```bash
docker run -p 8080:8080 \
  -e COSMOS_ENDPOINT=https://your-account.documents.azure.com:443/ \
  -e COSMOS_AUTH_MODE=KEY \
  -e COSMOS_KEY=your-primary-key \
  -e COSMOS_DEFAULT_DB=ureca_evo \
  cosmosdb-query-sidecar:1.0.0
```

### DefaultAzureCredential (Recommended for Production)

Uses Azure's credential chain - ideal for production with Managed Identity:

```bash
export COSMOS_ENDPOINT=https://your-account.documents.azure.com:443/
export COSMOS_AUTH_MODE=DEFAULT_AZURE_CREDENTIAL
export COSMOS_DEFAULT_DB=ureca_evo

# Start the sidecar
./gradlew bootRun
```

**Docker (with Azure CLI credentials for local development):**
```bash
docker run -p 8080:8080 \
  -e COSMOS_ENDPOINT=https://your-account.documents.azure.com:443/ \
  -e COSMOS_AUTH_MODE=DEFAULT_AZURE_CREDENTIAL \
  -e COSMOS_DEFAULT_DB=ureca_evo \
  -v ~/.azure:/root/.azure:ro \
  cosmosdb-query-sidecar:1.0.0
```

**Kubernetes (with Managed Identity / Workload Identity):**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cosmosdb-query-sidecar
spec:
  template:
    metadata:
      labels:
        azure.workload.identity/use: "true"
    spec:
      serviceAccountName: cosmosdb-sidecar-sa
      containers:
      - name: sidecar
        image: your-registry/cosmosdb-query-sidecar:1.0.0
        env:
        - name: COSMOS_ENDPOINT
          value: "https://your-account.documents.azure.com:443/"
        - name: COSMOS_AUTH_MODE
          value: "DEFAULT_AZURE_CREDENTIAL"
        - name: COSMOS_DEFAULT_DB
          value: "ureca_evo"
```

**DefaultAzureCredential Chain:**
The sidecar will try these authentication methods in order:
1. Environment variables (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID)
2. Workload Identity (when running in Kubernetes with proper federation)
3. Managed Identity (when running in Azure VM, App Service, Container Apps, etc.)
4. Azure CLI (for local development - `az login`)
5. Azure PowerShell
6. Interactive browser

### Switching Between Modes

Simply change the `COSMOS_AUTH_MODE` environment variable:

```bash
# Development with key
COSMOS_AUTH_MODE=KEY COSMOS_KEY=your-key ./gradlew bootRun

# Production with Managed Identity
COSMOS_AUTH_MODE=DEFAULT_AZURE_CREDENTIAL ./gradlew bootRun
```

### Required Permissions for DefaultAzureCredential

When using DefaultAzureCredential, ensure your identity has appropriate Cosmos DB permissions:

**Azure RBAC Roles (Recommended):**
- `Cosmos DB Built-in Data Contributor` - For read/write access
- `Cosmos DB Built-in Data Reader` - For read-only access

**Assign role using Azure CLI:**
```bash
# Get your Cosmos DB account resource ID
COSMOS_RESOURCE_ID=$(az cosmosdb show \
  --name your-cosmos-account \
  --resource-group your-rg \
  --query id -o tsv)

# Assign role to Managed Identity
az role assignment create \
  --assignee <managed-identity-client-id> \
  --role "Cosmos DB Built-in Data Contributor" \
  --scope $COSMOS_RESOURCE_ID
```

---

## Migration from Direct Cosmos DB SDK

### Before (Node.js with @azure/cosmos)

```javascript
const { CosmosClient } = require("@azure/cosmos");

const client = new CosmosClient({ endpoint, key });
const container = client.database("ureca_evo").container("users");

const { resources, requestCharge } = await container.items
  .query({
    query: "SELECT * FROM c WHERE c.userId = @userId",
    parameters: [{ name: "@userId", value: "u-001" }]
  })
  .fetchAll();
```

### After (Using Sidecar)

```javascript
const response = await fetch('http://localhost:8080/cosmos/v1/query/users?pk=u-001', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    sql: 'SELECT * FROM c WHERE c.userId = @userId',
    params: { userId: 'u-001' }
  })
});

const { data, cosmos } = await response.json();
const { results, count } = data;
const { ru } = cosmos;
```

**Benefits:**
- No need to manage Cosmos DB SDK in every service
- Centralized connection management
- Easier to monitor and optimize
- Language-agnostic (any HTTP client)

---

## Monitoring & Observability

### Check Health

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### Get Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

### Custom Metrics to Monitor

1. **Request Count by Container**
2. **Average RU per Query**
3. **Error Rate by Error Code**
4. **95th Percentile Latency**

---

## Tips & Best Practices

1. ✅ **Always provide partition key** when possible
2. ✅ **Use pagination** for large result sets
3. ✅ **Implement retry logic** for 429 errors
4. ✅ **Monitor RU consumption** via headers
5. ✅ **Use request IDs** for tracing
6. ✅ **Project only needed fields** in SELECT
7. ✅ **Use Direct mode** for best performance (default)
8. ❌ **Avoid cross-partition queries** when possible
9. ❌ **Don't fetch all data** at once without pagination
10. ❌ **Don't ignore error responses**
