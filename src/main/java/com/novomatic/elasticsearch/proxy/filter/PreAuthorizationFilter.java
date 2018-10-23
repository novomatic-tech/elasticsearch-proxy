package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.AuthorizationService;
import org.springframework.http.HttpStatus;

public class PreAuthorizationFilter extends ElasticsearchApiFilter {

    private final AuthorizationService authorizationService;

    public PreAuthorizationFilter(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public int filterOrder() {
        return Constants.PRE_AUTHORIZATION_FILTER_ORDER;
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        AuthorizationResult authorizationResult = authorizationService.authorize(getElasticsearchRequest());
        if (authorizationResult.isAuthorized()) {
            setPreAuthorizationResult(authorizationResult);
        } else {
            RequestContextExtensions.respondWith(currentContext, HttpStatus.FORBIDDEN);
        }
        return null;
    }
}
