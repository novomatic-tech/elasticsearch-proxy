package com.novomatic.elasticsearch.proxy;

public interface QueryScriptEvaluator {
    QueryScriptResult evaluateQueryScript(AuthorizationRuleParameters parameters, String queryScript);
}
