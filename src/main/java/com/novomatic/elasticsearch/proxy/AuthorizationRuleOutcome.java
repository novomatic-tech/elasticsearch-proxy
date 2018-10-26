package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import lombok.Data;

@Data
public class AuthorizationRuleOutcome {
    private final AuthorizationRule rule;
    private final String luceneQuery;
}
