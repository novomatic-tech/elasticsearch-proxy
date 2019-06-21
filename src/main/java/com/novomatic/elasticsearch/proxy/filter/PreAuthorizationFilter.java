package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.PreAuthorizationResult;
import com.novomatic.elasticsearch.proxy.AuthorizationService;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

@Slf4j
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
    public boolean shouldFilter() {
        return !isPassThrough() && isElasticsearchRequest();
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        PreAuthorizationResult preAuthorizationResult = authorizationService.authorize(getElasticsearchRequest());
        if (preAuthorizationResult.isAuthorized()) {
            log.debug("There are {} rule(s) matching the {} {} request", preAuthorizationResult.getMatchedRules().size(),
                    request.getMethod(), request.getRequestURI());
            setPreAuthorizationResult(preAuthorizationResult);
        } else {
            log.info("Authorization failed. There are no matching rules for the {} {} request.", request.getMethod(),
                    request.getRequestURI());
            throw new UnauthorizedException("The user is unauthorized to perform this operation");
        }
        return null;
    }


}
