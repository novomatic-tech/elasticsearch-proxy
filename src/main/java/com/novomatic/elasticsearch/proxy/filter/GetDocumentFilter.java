package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.DocumentEvaluator;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;

import static com.novomatic.elasticsearch.proxy.DocumentEvaluatorImpl.SOURCE_FIELD;
import static com.novomatic.elasticsearch.proxy.filter.Constants.RESPONSE_PROCESSING_FILTER;
import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.readResponseBody;
import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.respondWith;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;

@Slf4j
public class GetDocumentFilter extends ElasticsearchApiFilter {

    private final ObjectMapper objectMapper;
    private final DocumentEvaluator documentEvaluator;

    public GetDocumentFilter(ObjectMapper objectMapper, DocumentEvaluator documentEvaluator) {
        this.objectMapper = objectMapper;
        this.documentEvaluator = documentEvaluator;
    }

    @Override
    public String filterType() {
        return ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return RESPONSE_PROCESSING_FILTER;
    }

    @Override
    public boolean shouldFilter() {
        if (isPassThrough()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return request.usesMethod(HttpMethod.GET) &&
                request.hasIndices() &&
                request.hasTypes() &&
                request.hasResourceId() &&
                responseIs2xxSuccessfull();
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        try {
            String responseBody = readResponseBody(currentContext);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            AuthorizationResult authResult = getPreAuthorizationResult();
            JsonNode sourceNode = jsonNode.path(SOURCE_FIELD);
            if (sourceNode.isMissingNode()) {
                // TODO: decide what to do when document does not exist.
                setAuthorizationRan(true);
                return null;
            }
            String luceneQuery = authResult.getLuceneQuery();
            if (!documentEvaluator.matches(sourceNode, luceneQuery)) {
                log.info("Authorization failed. Document {} does not match query: {}", jsonNode.path("_id"), luceneQuery);
                throw new UnauthorizedException("The user is unauthorized to access this document");
            }
        } catch (IOException | ParseException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }

        setAuthorizationRan(true);
        return null;
    }

}
