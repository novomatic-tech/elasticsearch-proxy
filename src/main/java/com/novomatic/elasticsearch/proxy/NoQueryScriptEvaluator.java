package com.novomatic.elasticsearch.proxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoQueryScriptEvaluator implements QueryScriptEvaluator {
    @Override
    public QueryScriptResult evaluateQueryScript(AuthorizationRuleParameters parameters, String queryScript) {
        log.warn("Query script evaluation is currently not supported");
        throw new UnauthorizedException("Query script evaluation is not supported");
    }
}
