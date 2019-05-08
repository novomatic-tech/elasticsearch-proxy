package com.novomatic.elasticsearch.proxy.filter;

import com.novomatic.elasticsearch.proxy.PreAuthorizationResult;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import org.springframework.http.HttpMethod;

public class CheckIndexFilter extends ElasticsearchApiFilter {

    @Override
    public boolean shouldFilter() {
        if (isPassThrough() || !isElasticsearchRequest()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return request.usesMethod(HttpMethod.HEAD) &&
                request.hasIndices() &&
                !request.hasTypes() &&
                !request.hasResourceId();
    }

    @Override
    public Object run() {
        PreAuthorizationResult authResult = getPreAuthorizationResult();
        if (!authResult.isAuthorized()) {
            throw new UnauthorizedException("The user is unauthorized to check this index");
        }
        setAuthorizationRan(true);
        return null;
    }
}
