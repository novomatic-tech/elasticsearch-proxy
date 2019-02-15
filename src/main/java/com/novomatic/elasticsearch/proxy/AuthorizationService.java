package com.novomatic.elasticsearch.proxy;

public interface AuthorizationService {
    PreAuthorizationResult authorize(ElasticsearchRequest request);
}
