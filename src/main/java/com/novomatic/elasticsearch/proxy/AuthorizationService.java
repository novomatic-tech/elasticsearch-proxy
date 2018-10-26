package com.novomatic.elasticsearch.proxy;

public interface AuthorizationService {
    AuthorizationResult authorize(ElasticsearchRequest request);
}
