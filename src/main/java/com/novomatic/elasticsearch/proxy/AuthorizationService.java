package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.ResourceAction;

import java.util.Set;

public interface AuthorizationService {
    AuthorizationResult authorize(Principal principal, Set<String> indices, ResourceAction action);
    default AuthorizationResult authorize(ElasticsearchRequest request) {
        return authorize(
                request.getPrincipal(),
                request.getIndices(),
                request.deduceResourceAction());
    }
}
