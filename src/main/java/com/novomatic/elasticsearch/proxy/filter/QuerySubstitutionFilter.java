package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.ElasticsearchQuery;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class QuerySubstitutionFilter extends ElasticsearchApiFilter {

    @Override
    public boolean shouldFilter() {
        if (isPassThrough()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return request.usesOneOfMethods(HttpMethod.GET, HttpMethod.POST) &&
                (request.isSearchOperation() ||
                request.isCountOperation() ||
                request.isDeleteByQueryOperation() ||
                request.isUpdateByQueryOperation());
    }

    @Override
    public Object run() {
        ElasticsearchRequest request = getElasticsearchRequest();
        ElasticsearchQuery incomingQuery = extractElasticsearchQuery();
        ElasticsearchQuery authQuery = getAuthorizationQuery(incomingQuery);
        ElasticsearchQuery modifiedQuery = incomingQuery.and(authQuery);
        // TODO: this may be optimized according to boolean logic: A and (A or B or C or ...) = A

        String newBody = modifiedQuery.asOuterJson();
        log.debug("Modified body for the {} {} request:\n {}", request.getMethod(), request.getRequestURI(), newBody);
        request.setBody(newBody);
        setAuthorizationRan(true);
        return null;
    }

    private ElasticsearchQuery getAuthorizationQuery(ElasticsearchQuery incomingQuery) {
        AuthorizationResult authorizationResult = getPreAuthorizationResult();
        if (incomingQuery.isIndexAggregation()) {
            return ElasticsearchQuery.allowedIndices(authorizationResult.getAllowedIndices());
        }
        return authorizationResult.getQuery();
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
