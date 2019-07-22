package com.novomatic.elasticsearch.proxy.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class ResourcesConstraints {
    private Set<String> indices = Collections.emptySet();
    private String query;
    private String queryScript;
    public boolean hasQueryScript() {
        return queryScript != null;
    }
    public boolean hasQuery() {
        return query != null;
    }
}
