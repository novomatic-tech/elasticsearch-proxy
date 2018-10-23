package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.ElasticsearchQuery;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

public class QuerySubstitutionFilter extends ElasticsearchApiFilter {

    @Override
    public boolean shouldFilter() {
        if (isPassThrough()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return request.isSearch() || request.isCount() || request.isDeleteByQuery() || request.isUpdateByQuery();
    }

    @Override
    public Object run() {
        AuthorizationResult authorizationResult = getPreAuthorizationResult();
        ElasticsearchRequest request = getElasticsearchRequest();
        ElasticsearchQuery incomingQuery = extractElasticsearchQuery();

        ElasticsearchQuery modifiedQuery = incomingQuery.and(authorizationResult.getQuery());
        // TODO: this may be optimized according to boolean logic: A and (A or B or C or ...) = A

        request.setBody(modifiedQuery.asOuterJson());
        return null;
    }

    protected ElasticsearchQuery extractElasticsearchQuery() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        Optional<String> requestBody = RequestContextExtensions.readRequestBody(currentContext);
        if (requestBody.isPresent()) {
            try {
                return ElasticsearchQuery.fromJson(requestBody.get());
            } catch (IOException e) {
                ReflectionUtils.rethrowRuntimeException(e);
            }
        } else if (request.getMethod().equalsIgnoreCase("GET")) {
            String luceneQuery = request.getParameter("q");
            return luceneQuery == null
                    ? ElasticsearchQuery.empty()
                    : ElasticsearchQuery.fromLuceneQuery(luceneQuery);
        }
        return ElasticsearchQuery.empty();
    }
}
