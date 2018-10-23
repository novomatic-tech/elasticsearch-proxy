package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.AuthorizationResult;
import com.novomatic.elasticsearch.proxy.DocumentEvaluator;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;

import static com.novomatic.elasticsearch.proxy.DocumentEvaluatorImpl.SOURCE_FIELD;
import static com.novomatic.elasticsearch.proxy.filter.Constants.RESPONSE_PROCESSING_FILTER;
import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.readResponseBody;
import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.respondWith;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;

public class MultiGetFilter extends ElasticsearchApiFilter {

    private final ObjectMapper objectMapper;
    private final DocumentEvaluator documentEvaluator;

    public MultiGetFilter(DocumentEvaluator documentEvaluator) {
        this.documentEvaluator = documentEvaluator;
        this.objectMapper = new ObjectMapper();
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
        return (request.getMethod().equalsIgnoreCase("GET") ||
                request.getMethod().equalsIgnoreCase("POST")) &&
               request.isMultiGet();
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
            String luceneQuery = authResult.getLuceneQuery().orElse(null);
            if (!docsNode.isMissingNode() && docsNode instanceof ArrayNode) {
                for (JsonNode docNode : docsNode) {
                    JsonNode sourceNode = docNode.path(SOURCE_FIELD);
                    if (sourceNode.isMissingNode()) {
                        // TODO: decide what to do when document does not exist.
                        continue;
                    }
                    if (!documentEvaluator.matches(sourceNode, luceneQuery)) {
                        respondWith(currentContext, HttpStatus.FORBIDDEN);
                        break;
                    }
                }
            }
        } catch (IOException | ParseException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        return null;
    }
}
