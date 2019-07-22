package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.PreAuthorizationResult;
import com.novomatic.elasticsearch.proxy.ElasticsearchQuery;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.readRequestBody;

@Slf4j
public class MultiSearchFilter extends ElasticsearchApiFilter {

    @Override
    public boolean shouldFilter() {
        if (isPassThrough() || !isElasticsearchRequest()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return request.usesOneOfMethods(HttpMethod.GET, HttpMethod.POST) && request.isMultiSearchOperation();
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        ElasticsearchRequest request = getElasticsearchRequest();
        PreAuthorizationResult authResult = getPreAuthorizationResult();
        Set<String> indices = request.getIndices();
        try {
            Optional<String> requestBody = readRequestBody(currentContext);
            if (!requestBody.isPresent()) {
                return null;
            }
            String[] bodyParts = requestBody.get().split("\\r?\\n");
            StringBuilder modifiedBody = new StringBuilder();
            for (int i = 0; i < bodyParts.length; i++) {
                String bodyPart = bodyParts[i];

                if (i % 2 == 0) {
                    indices = ElasticsearchQuery.extractIndex(bodyPart).map(Collections::singleton).orElse(request.getIndices());
                } else {
                    ElasticsearchQuery query = ElasticsearchQuery.fromJson(bodyPart);
                    ElasticsearchQuery modifiedQuery = query.and(authResult.getQueryFor(indices));
                    bodyPart = modifiedQuery.asOuterJson();

                    // TODO: authResult.getQuery() can be optimized - limit it by calling authorization service on a per-index basis.
                }
                modifiedBody.append(bodyPart);
                modifiedBody.append("\n");
            }

            // TODO: Adjust highlight.fields property to not include authorization queries in case they were not asked for.

            log.debug("Modified body for the {} {} request:\n {}", request.getMethod(), request.getRequestURI(), modifiedBody.toString());
            getElasticsearchRequest().setBody(modifiedBody.toString());
        } catch (IOException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }

        setAuthorizationRan(true);
        return null;
    }
}
