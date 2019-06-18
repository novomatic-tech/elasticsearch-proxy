package com.novomatic.elasticsearch.proxy;

import lombok.Getter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

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

    public static PreAuthorizationResult unauthorized() {
        return UNAUTHORIZED;
    }

    public static PreAuthorizationResult authorized(List<AuthorizationRuleOutcome> matchedRules) {
        if (matchedRules.isEmpty()) {
            throw new IllegalArgumentException("A collection of matched rules must have at least one item.");
        }
        return new PreAuthorizationResult(matchedRules);
    }

    public Set<String> getAllowedIndices() {
        return matchedRules.stream()
                .flatMap(outcome -> outcome.getRule().getResources().getIndices().stream())
                .collect(Collectors.toSet());
    }

    public ElasticsearchQuery getQuery() {
        return ElasticsearchQuery.fromLuceneQuery(getLuceneQuery().toString());
    }

    public Query getLuceneQuery() {
        if (matchedRules.isEmpty()) {
            return new MatchAllDocsQuery();
        }
        BooleanQuery.Builder luceneQueryBuilder = matchedRules.stream()
                .map(AuthorizationRuleOutcome::getLuceneQuery)
                .collect(BooleanQuery.Builder::new,
                        (builder, query) -> builder.add(query, BooleanClause.Occur.SHOULD),
                        (builder1, builder2) -> builder1.add(builder2.build(), BooleanClause.Occur.SHOULD));

        return luceneQueryBuilder.build();
    }

    public List<AuthorizationRuleOutcome> getMatchedRules() {
        return matchedRules;
    }
}
