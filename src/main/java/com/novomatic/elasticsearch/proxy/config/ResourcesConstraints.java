package com.novomatic.elasticsearch.proxy.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class ResourcesConstraints {
    private List<String> indices = Collections.emptyList();
    private String query;
    private String queryScript;
    public boolean hasQueryScript() {
        return queryScript != null;
    }
    public boolean hasQuery() {
        return query != null;
    }
}
