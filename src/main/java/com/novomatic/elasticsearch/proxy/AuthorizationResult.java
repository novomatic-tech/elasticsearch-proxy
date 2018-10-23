package com.novomatic.elasticsearch.proxy;

import lombok.Data;

import java.util.Optional;

@Data
public class AuthorizationResult {

    private static AuthorizationResult UNAUTHORIZED = new AuthorizationResult(null, false);
    private final ElasticsearchQuery query;
    private final Optional<String> luceneQuery;
    private final boolean authorized;

    private AuthorizationResult(String luceneQuery, boolean authorized) {
        this.luceneQuery = Optional.ofNullable(luceneQuery);
        this.query = ElasticsearchQuery.fromLuceneQuery(luceneQuery);
        this.authorized = authorized;
    }

    public static AuthorizationResult unauthorized() {
        return UNAUTHORIZED;
    }

    public static AuthorizationResult authorized() {
        return new AuthorizationResult(null, true);
    }

    public static AuthorizationResult authorized(String luceneQuery) {
        return new AuthorizationResult(luceneQuery, true);
    }
}
