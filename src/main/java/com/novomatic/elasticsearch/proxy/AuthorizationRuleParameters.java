package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class AuthorizationRuleParameters {

    private final ElasticsearchRequest request;
    private final Principal principal;
    private final List<AuthorizationRule> matchingRules;

    public Set<String> getMatchingIndices() {
        return matchingRules.stream()
            .flatMap(rule -> rule.getResources().getIndices().stream())
            .collect(Collectors.toSet());
    }
}
