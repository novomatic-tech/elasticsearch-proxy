package com.novomatic.elasticsearch.proxy.filter;

import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import com.novomatic.elasticsearch.proxy.config.ResourceAction;

import static com.novomatic.elasticsearch.proxy.filter.Constants.AUTHORIZATION_FILTER_ORDER;

public class WritePostAuthorizationFilter extends ElasticsearchApiFilter {

    @Override
    public int filterOrder() {
        return AUTHORIZATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        return !isPassThrough() &&
               !isAuthorizationRan() &&
               getElasticsearchRequest().deduceResourceAction().equals(ResourceAction.WRITE);
    }

    @Override
    public Object run() {
        throw new UnauthorizedException("The user is unauthorized to perform this operation");
    }
}
