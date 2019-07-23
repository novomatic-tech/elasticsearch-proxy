package com.novomatic.elasticsearch.proxy;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PreAuthorizationResult {

    private static PreAuthorizationResult UNAUTHORIZED = new PreAuthorizationResult();

    @Getter
    private final boolean authorized;
    private final List<AuthorizationRuleOutcome> matchedRules;

    private PreAuthorizationResult() {
        authorized = false;
        matchedRules = Collections.emptyList();
    }

    private PreAuthorizationResult(List<AuthorizationRuleOutcome> matchedRules) {
        this.authorized = true;
        this.matchedRules = matchedRules;
    }

    public Set<String> getAllowedIndices() {
        return matchedRules.stream()
                .flatMap(outcome -> outcome.getRule().getResources().getIndices().stream())
                .collect(Collectors.toSet());
    }

    public ElasticsearchQuery getQuery() {
        return ElasticsearchQuery.fromLuceneQuery(getLuceneQuery());
    }

    public ElasticsearchQuery getQueryFor(Set<String> indices) {
        List<AuthorizationRuleOutcome> filteredRules = matchedRules.stream()
                .filter(rule -> indices.isEmpty() || rule.getRule().getResources().getIndices().equals(indices))
                .collect(Collectors.toList());
        String luceneQuery = getLuceneQuery(filteredRules);
        return ElasticsearchQuery.fromLuceneQuery(luceneQuery);
    }

    public String getLuceneQuery() {
        return getLuceneQuery(matchedRules);
    }

    public List<AuthorizationRuleOutcome> getMatchedRules() {
        return matchedRules;
    }

    public static PreAuthorizationResult unauthorized() {
        return UNAUTHORIZED;
    }

    public static PreAuthorizationResult authorized(List<AuthorizationRuleOutcome> matchedRules) {
        if (matchedRules.isEmpty()) {
            throw new IllegalArgumentException("A collection of matched rules must have at least one item.");
        }
        return new PreAuthorizationResult(matchedRules);
    }

    private static String getLuceneQuery(List<AuthorizationRuleOutcome> matchedRules) {
        List<String> luceneQueries = matchedRules.stream()
                .map(AuthorizationRuleOutcome::getLuceneQuery)
                .map(PreAuthorizationResult::wrapLuceneQuery)
                .collect(Collectors.toList());
        if (luceneQueries.isEmpty()) {
            return null;
        }
        return String.join(" OR ", luceneQueries);
    }

    private static String wrapLuceneQuery(String luceneQuery) {
        return "(" + luceneQuery + ")";
    }
}
