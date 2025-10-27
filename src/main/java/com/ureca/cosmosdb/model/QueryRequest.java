package com.ureca.cosmosdb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String sql;
    private Map<String, Object> params;
}
