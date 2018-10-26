package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationProperties;
import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import com.novomatic.elasticsearch.proxy.config.ResourceAction;
import com.novomatic.elasticsearch.proxy.config.ResourcesConstraints;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
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
                .filter(principal::fulfills)
                .filter(rule -> rule.getActions().contains(action))
                .filter(rule -> rule.isApplicableForIndices(indices))
                .collect(Collectors.toList());
    }

    private List<AuthorizationRuleOutcome> processAuthorizationRules(AuthorizationRuleParameters parameters) {
        return parameters.getMatchingRules().stream()
                .map(rule -> new AuthorizationRuleOutcome(rule, extractLuceneQuery(parameters, rule).orElse("*")))
                .collect(Collectors.toList());
    }

    private Optional<String> extractLuceneQuery(AuthorizationRuleParameters parameters, AuthorizationRule authorizationRule) {
        ResourcesConstraints resourcesConstraints = authorizationRule.getResources();
        if (resourcesConstraints.hasQueryScript()) {
            QueryScriptResult result = queryScriptEvaluator.evaluateQueryScript(parameters, resourcesConstraints.getQueryScript());
            return Optional.ofNullable(result.getLuceneQuery());
        }
        if (resourcesConstraints.hasQuery()) {
            return Optional.ofNullable(resourcesConstraints.getQuery());
        }
        return Optional.empty();
    }

    @Override
    public AuthorizationResult authorize(ElasticsearchRequest request) {
        ResourceAction action = request.deduceResourceAction();
        Set<String> indices = request.getIndices();
        Principal principal = request.getPrincipal();

        log.debug("Processing authorization for a {} action on {} indices", action, indices);
        List<AuthorizationRule> authorizationRules = getMatchingAuthorizationRules(principal, indices, action);
        if (authorizationRules.isEmpty()) {
            return AuthorizationResult.unauthorized();
        }

        AuthorizationRuleParameters parameters = new AuthorizationRuleParameters(request, principal, authorizationRules);
        List<AuthorizationRuleOutcome> authorizationRuleOutcomes = processAuthorizationRules(parameters);
        return AuthorizationResult.authorized(authorizationRuleOutcomes);
    }
}
