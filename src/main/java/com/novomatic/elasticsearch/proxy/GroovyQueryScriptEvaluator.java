package com.novomatic.elasticsearch.proxy;

import groovy.lang.Binding;
import groovy.lang.GString;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GroovyQueryScriptEvaluator implements QueryScriptEvaluator {

    @Override
    public QueryScriptResult evaluateQueryScript(AuthorizationRuleParameters parameters, String queryScript) {
        Binding binding = new Binding();
        binding.setVariable("request", parameters.getRequest());
        binding.setVariable("principal", parameters.getPrincipal());
        binding.setVariable("matchingIndices", parameters.getMatchingIndices());
        binding.setVariable("matchingRules", parameters.getMatchingRules());
        GroovyShell shell = new GroovyShell(binding);
        try {
            Object answer = shell.evaluate(queryScript);
            String luceneQuery = getLuceneQuery(answer);
            return new QueryScriptResult(luceneQuery);
        } catch (Exception ex) {
            throw new UnauthorizedException("The user is unauthorized to perform this action (query script failure).", ex);
        }
    }

    private String getLuceneQuery(Object answer) {
        if (answer == null) {
            return null;
        }
        if ((answer instanceof GString) || (answer instanceof String)) {
            String value = answer.toString();
            return value.equals("") ? null : value;
        } else if (answer instanceof Map) {
            List<String> queryPart = ((Map<?,?>)answer).entrySet().stream().map(kv -> kv.getKey() + ":\"" + kv.getValue() + "\"")
                    .collect(Collectors.toList());
            return String.join(" AND ", queryPart);
        } else {
            throw new IllegalArgumentException("Returned value must be one of (GString, String or Map)");
        }
    }
}
