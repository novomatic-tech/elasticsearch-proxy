package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import lombok.Data;
import org.apache.lucene.search.Query;

@Data
public class AuthorizationRuleOutcome {
    private final AuthorizationRule rule;
    private final Query luceneQuery;
}
