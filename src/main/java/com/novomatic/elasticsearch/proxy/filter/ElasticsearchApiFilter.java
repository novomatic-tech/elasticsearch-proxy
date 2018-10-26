package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import org.springframework.http.HttpStatus;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

abstract class ElasticsearchApiFilter extends ZuulFilter {

    @Override
    public boolean shouldFilter() {
        return !isPassThrough();
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return Constants.AUTHORIZATION_FILTER_ORDER;
    }

    protected ElasticsearchRequest getElasticsearchRequest() {
        return (ElasticsearchRequest) RequestContext.getCurrentContext().getRequest();
    }

    protected boolean isPassThrough() {
        return RequestContext.getCurrentContext().getBoolean(Constants.PASS_THROUGH);
    }

    protected void setPassThrough(boolean passThrough) {
        RequestContext.getCurrentContext().set(Constants.PASS_THROUGH, passThrough);
    }

    protected AuthorizationResult getPreAuthorizationResult() {
        return (AuthorizationResult)RequestContext.getCurrentContext().get(Constants.AUTHORIZATION);
    }

    protected void setPreAuthorizationResult(AuthorizationResult result) {
        RequestContext.getCurrentContext().set(Constants.AUTHORIZATION, result);
    }

    protected boolean responseIs2xxSuccessfull() {
        return HttpStatus.valueOf(RequestContext.getCurrentContext().getResponseStatusCode()).is2xxSuccessful();
    }

    protected boolean isAuthorizationRan() {
        return RequestContext.getCurrentContext().getBoolean("authorizationRan");
    }

    protected void setAuthorizationRan(boolean value) {
        RequestContext.getCurrentContext().set("authorizationRan", value);
    }
}
