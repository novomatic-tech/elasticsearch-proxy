package com.novomatic.elasticsearch.proxy.filter;

import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import com.novomatic.elasticsearch.proxy.config.ResourceAction;

import static com.novomatic.elasticsearch.proxy.filter.Constants.RESPONSE_PROCESSING_FILTER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;

public class ReadPostAuthorizationFilter extends ElasticsearchApiFilter {

    @Override
    public String filterType() {
        return ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return RESPONSE_PROCESSING_FILTER + 1;
    }

    @Override
    public boolean shouldFilter() {
        return !isPassThrough() &&
                isElasticsearchRequest() &&
                !isAuthorizationRan() &&
                getElasticsearchRequest().deduceResourceAction().equals(ResourceAction.READ);
    }

    @Override
    public Object run() {
        throw new UnauthorizedException("The user is unauthorized to perform this operation");
    }
}
