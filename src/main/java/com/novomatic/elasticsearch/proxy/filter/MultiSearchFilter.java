package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.ElasticsearchQuery;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.util.Optional;

import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.readRequestBody;

public class MultiSearchFilter extends ElasticsearchApiFilter {

    @Override
    public boolean shouldFilter() {
        if (isPassThrough()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return (request.getMethod().equalsIgnoreCase("GET") ||
                request.getMethod().equalsIgnoreCase("POST")) &&
                request.isMultiSearch();
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        AuthorizationResult authResult = getPreAuthorizationResult();
        try {
            Optional<String> requestBody = readRequestBody(currentContext);
            if (!requestBody.isPresent()) {
                return null;
            }
            String[] bodyParts = requestBody.get().split("\\r?\\n");
            StringBuilder modifiedBody = new StringBuilder();
            for (int i = 0; i < bodyParts.length; i++) {
                String bodyPart = bodyParts[i];
                if (i % 2 != 0) {
                    ElasticsearchQuery query = ElasticsearchQuery.fromJson(bodyPart);
                    ElasticsearchQuery modifiedQuery = query.and(authResult.getQuery());
                    bodyPart = modifiedQuery.asOuterJson();

                    // TODO: authResult.getQuery() can be optimized - limit it by calling authorization service on a per-index basis.
                }
                modifiedBody.append(bodyPart);
                modifiedBody.append("\n");
            }

            // TODO: Adjust highlight.fields property to not include authorization queries in case they were not asked for.
            getElasticsearchRequest().setBody(modifiedBody.toString());
        } catch (IOException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        return null;
    }
}
