package com.novomatic.elasticsearch.proxy;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorizationResult {

    private static AuthorizationResult UNAUTHORIZED = new AuthorizationResult();

    @Getter
    private final boolean authorized;
    private final List<AuthorizationRuleOutcome> matchedRules;

    private AuthorizationResult() {
        authorized = false;
        matchedRules = Collections.emptyList();
    }

    private AuthorizationResult(List<AuthorizationRuleOutcome> matchedRules) {
        this.authorized = true;
        this.matchedRules = matchedRules;
    }

    public static AuthorizationResult unauthorized() {
        return UNAUTHORIZED;
    }

    public static AuthorizationResult authorized(List<AuthorizationRuleOutcome> matchedRules) {
        if (matchedRules.isEmpty()) {
            throw new IllegalArgumentException("A collection of matched rules must have at least one item.");
        }
        return new AuthorizationResult(matchedRules);
    }

    public Set<String> getAllowedIndices() {
        return matchedRules.stream()
                .flatMap(outcome -> outcome.getRule().getResources().getIndices().stream())
                .collect(Collectors.toSet());
    }

    public ElasticsearchQuery getQuery() {
        return ElasticsearchQuery.fromLuceneQuery(getLuceneQuery());
    }

    public String getLuceneQuery() {
        List<String> luceneQueries = matchedRules.stream()
                .map(AuthorizationRuleOutcome::getLuceneQuery)
                .filter(Objects::nonNull)
                .map(this::wrapLuceneQuery)
                .collect(Collectors.toList());
        if (luceneQueries.isEmpty()) {
            return null;
        }
        return String.join(" OR ", luceneQueries);
    }

    private String wrapLuceneQuery(String luceneQuery) {
        return "(" + luceneQuery + ")";
    }

    public List<AuthorizationRuleOutcome> getMatchedRules() {
        return matchedRules;
    }
}
