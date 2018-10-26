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

    public AuthorizationServiceImpl(AuthorizationProperties authorizationProperties) {
        this.authorizationProperties = authorizationProperties;
    }

    @Override
    public AuthorizationResult authorize(Principal principal, Set<String> indices, ResourceAction action) {
        log.debug("Processing authorization for a {} action on {} indices", action, indices);
        List<AuthorizationRule> authorizationRules = getMatchingAuthorizationRules(principal, indices, action);
        if (authorizationRules.isEmpty()) {
            return AuthorizationResult.unauthorized();
        }
        List<AuthorizationRuleOutcome> authorizationRuleOutcomes = processAuthorizationRules(authorizationRules);
        return AuthorizationResult.authorized(authorizationRuleOutcomes);
    }

    private List<AuthorizationRule> getMatchingAuthorizationRules(Principal principal, Set<String> indices, ResourceAction action) {
        return authorizationProperties.getAllow().stream()
                .filter(principal::fulfills)
                .filter(rule -> rule.getActions().contains(action))
                .filter(rule -> rule.isApplicableForIndices(indices))
                .collect(Collectors.toList());
    }

    private List<AuthorizationRuleOutcome> processAuthorizationRules(List<AuthorizationRule> authorizationRules) {
        return authorizationRules.stream()
                .map(rule -> new AuthorizationRuleOutcome(rule, extractLuceneQuery(rule).orElse("*")))
                .collect(Collectors.toList());
    }

    private Optional<String> extractLuceneQuery(AuthorizationRule authorizationRule) {
        ResourcesConstraints resourcesConstraints = authorizationRule.getResources();
        if (resourcesConstraints.hasQueryScript()) {
            // TODO: invoke query script on current principal
        }
        if (resourcesConstraints.hasQuery()) {
            return Optional.ofNullable(resourcesConstraints.getQuery());
        }
        return Optional.empty();
    }
}
