package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationProperties;
import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import com.novomatic.elasticsearch.proxy.config.ResourceAction;
import com.novomatic.elasticsearch.proxy.config.ResourcesConstraints;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorizationServiceImpl implements AuthorizationService {

    private final AuthorizationProperties authorizationProperties;

    public AuthorizationServiceImpl(AuthorizationProperties authorizationProperties) {
        this.authorizationProperties = authorizationProperties;
    }

    @Override
    public AuthorizationResult authorize(Principal principal, Set<String> indices, ResourceAction action) {
        List<AuthorizationRule> authorizationRules = getMatchingAuthorizationRules(principal, indices, action);
        if (authorizationRules.isEmpty()) {
            return AuthorizationResult.unauthorized();
        }
        String query = buildLuceneQuery(authorizationRules);
        return AuthorizationResult.authorized(query);
    }

    private List<AuthorizationRule> getMatchingAuthorizationRules(Principal principal, Set<String> indices, ResourceAction action) {
        return authorizationProperties.getAllow().stream()
                .filter(principal::fulfills)
                .filter(rule -> rule.getActions().contains(action))
                .filter(rule -> rule.isApplicableForIndices(indices))
                .collect(Collectors.toList());
    }

    private String buildLuceneQuery(List<AuthorizationRule> authorizationRules) {
        List<String> luceneQueries = authorizationRules.stream()
                .map(this::extractLuceneQuery)
                .filter(Optional::isPresent)
                .map(q -> wrapLuceneQuery(q.get()))
                .collect(Collectors.toList());
        return luceneQueries.isEmpty()
                ? null
                : String.join(" OR ", luceneQueries);
    }

    private String wrapLuceneQuery(String luceneQuery) {
        return "(" + luceneQuery + ")";
    }

    private Optional<String> extractLuceneQuery(AuthorizationRule authorizationRule) {
        ResourcesConstraints resourcesConstraints = authorizationRule.getResources();
        if (resourcesConstraints.hasQueryScript()) {
            // TODO: invoke query script
            return Optional.empty();
        }
        if (resourcesConstraints.hasQuery()) {
            return Optional.of(resourcesConstraints.getQuery());
        }
        return Optional.empty();
    }
}
