package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.AuthorizationService;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;

import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.respondWith;

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
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        AuthorizationResult authorizationResult = authorizationService.authorize(getElasticsearchRequest());
        if (authorizationResult.isAuthorized()) {
            log.debug("There are {} rule(s) matching the {} {} request", authorizationResult.getMatchedRules().size(),
                    request.getMethod(), request.getRequestURI());
            setPreAuthorizationResult(authorizationResult);
        } else {
            log.info("Authorization failed. There are no matching rules for the {} {} request.", request.getMethod(),
                    request.getRequestURI());
            throw new UnauthorizedException("The user is unauthorized to perform this operation");
        }
        return null;
    }
}
