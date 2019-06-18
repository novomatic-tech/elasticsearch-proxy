package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationProperties;
import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import com.novomatic.elasticsearch.proxy.config.ResourceAction;
import com.novomatic.elasticsearch.proxy.config.ResourcesConstraints;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

    private final AuthorizationProperties authorizationProperties;
    private final QueryScriptEvaluator queryScriptEvaluator;

    public AuthorizationServiceImpl(AuthorizationProperties authorizationProperties, QueryScriptEvaluator queryScriptEvaluator) {
        this.authorizationProperties = authorizationProperties;
        this.queryScriptEvaluator = queryScriptEvaluator;
    }

    private List<AuthorizationRule> getMatchingAuthorizationRules(Principal principal, Set<String> indices, ResourceAction action) {
        return authorizationProperties.getAllow().stream()
                .filter(rule -> principal.fulfills(rule.getPrincipal()))
                .filter(rule -> rule.getActions().contains(action))
                .filter(rule -> rule.isApplicableForIndices(indices))
                .collect(Collectors.toList());
    }

    private List<AuthorizationRuleOutcome> processAuthorizationRules(AuthorizationRuleParameters parameters) {
        return parameters.getMatchingRules().stream()
                .map(rule -> new AuthorizationRuleOutcome(rule, extractLuceneQuery(parameters, rule)))
                .collect(Collectors.toList());
    }

    private Query extractLuceneQuery(AuthorizationRuleParameters parameters, AuthorizationRule authorizationRule) {
        ResourcesConstraints resourcesConstraints = authorizationRule.getResources();
        if (resourcesConstraints.hasQueryScript()) {
            QueryScriptResult result = queryScriptEvaluator.evaluateQueryScript(parameters, resourcesConstraints.getQueryScript());
            return result.getLuceneQuery().map(this::parseQuery).orElse(new MatchNoDocsQuery());
        }
        if (resourcesConstraints.hasQuery()) {
            return parseQuery(resourcesConstraints.getQuery());
        }
        return new MatchAllDocsQuery();
    }

    @Override
    public PreAuthorizationResult authorize(ElasticsearchRequest request) {
        ResourceAction action = request.deduceResourceAction();
        Set<String> indices = request.getIndices();
        Principal principal = request.getPrincipal();

        log.debug("Processing authorization for a {} action on {} indices", action, indices);
        List<AuthorizationRule> authorizationRules = getMatchingAuthorizationRules(principal, indices, action);
        if (authorizationRules.isEmpty()) {
            return PreAuthorizationResult.unauthorized();
        }

        AuthorizationRuleParameters parameters = new AuthorizationRuleParameters(request, principal, authorizationRules);
        List<AuthorizationRuleOutcome> authorizationRuleOutcomes = processAuthorizationRules(parameters);
        return PreAuthorizationResult.authorized(authorizationRuleOutcomes);
    }

    private Query parseQuery(String queryAsString) {
        QueryParser queryParser = new QueryParser("text", new KeywordAnalyzer());
        try {
            return queryParser.parse(queryAsString);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
}
