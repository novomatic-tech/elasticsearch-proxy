package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ERROR_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_ERROR_FILTER_ORDER;

public class WrapErrorFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_ERROR_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getThrowable() != null;
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        Throwable throwable = currentContext.getThrowable();
        while (throwable != null && !(throwable instanceof UnauthorizedException)) {
            throwable = throwable.getCause();
        }
        if (throwable != null) {
            currentContext.setThrowable(new ZuulException(throwable, "Forbidden", 403, throwable.getMessage()));
        }
        return null;
    }
}
