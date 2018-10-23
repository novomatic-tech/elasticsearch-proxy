package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.http.HttpHeaders;

import java.util.Collections;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

public class NoDefaultEncodingFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return SIMPLE_HOST_ROUTING_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        boolean hasAcceptEncoding = Collections.list(context.getRequest().getHeaderNames()).stream()
            .anyMatch(h -> h.equalsIgnoreCase(HttpHeaders.ACCEPT_ENCODING));
        if (!hasAcceptEncoding) {
            context.addZuulRequestHeader(HttpHeaders.ACCEPT_ENCODING, "identity");
        }
        return null;
    }
}
