package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
public class MultiGetFilter extends ElasticsearchApiFilter {

    private final ObjectMapper objectMapper;
    private final DocumentEvaluator documentEvaluator;

    public MultiGetFilter(ObjectMapper objectMapper, DocumentEvaluator documentEvaluator) {
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
        return request.usesOneOfMethods(HttpMethod.GET, HttpMethod.POST) &&
                request.isMultiGetOperation() &&
                responseIs2xxSuccessfull();
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        try {
            String responseBody = readResponseBody(currentContext);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            AuthorizationResult authResult = getPreAuthorizationResult();
            // TODO: authResult.getLuceneQuery() can be optimized - limit it by calling authorization service on a per-index basis.

            JsonNode docsNode = jsonNode.path("docs");
            String luceneQuery = authResult.getLuceneQuery();
            if (!docsNode.isMissingNode() && docsNode instanceof ArrayNode) {
                for (JsonNode docNode : docsNode) {
                    JsonNode sourceNode = docNode.path(SOURCE_FIELD);
                    if (sourceNode.isMissingNode()) {
                        // TODO: decide what to do when document does not exist.
                        continue;
                    }
                    if (!documentEvaluator.matches(sourceNode, luceneQuery)) {
                        log.info("Authorization failed. Document {} does not match query: {}", docNode.path("_id"), luceneQuery);
                        throw new UnauthorizedException("The user is unauthorized to access at least one requested document");
                    }
                }
            }
        } catch (IOException | ParseException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }

        setAuthorizationRan(true);
        return null;
    }
}
