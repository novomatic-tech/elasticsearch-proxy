package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.context.RequestContext;
import com.novomatic.elasticsearch.proxy.PreAuthorizationResult;
import com.novomatic.elasticsearch.proxy.DocumentEvaluator;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.util.Optional;

import static com.novomatic.elasticsearch.proxy.filter.RequestContextExtensions.readRequestBody;

@Slf4j
public class WriteDocumentFilter extends ElasticsearchApiFilter {

    private final ObjectMapper objectMapper;
    private final DocumentEvaluator documentEvaluator;

    public WriteDocumentFilter(ObjectMapper objectMapper, DocumentEvaluator documentEvaluator) {
        this.objectMapper = objectMapper;
        this.documentEvaluator = documentEvaluator;
    }

    @Override
    public boolean shouldFilter() {
        if (isPassThrough() || !isElasticsearchRequest()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        if (!request.usesOneOfMethods(HttpMethod.POST, HttpMethod.PUT)) {
            return false;
        }
        boolean answer = request.hasExplicitIndices() &&
                request.hasTypes() &&
                (request.hasResourceId() || (request.usesMethod(HttpMethod.POST) && !request.hasOperation()));

        boolean isPartialUpdate = request.hasResourceId() && request.isUpdateOperation();
        if (isPartialUpdate) {
            log.warn("Requested partial document update ({}) is not supported and hence the user is unauthorized to perform this action.",
                    request.getRequestURI());

            // TODO: handle partial updates!
        }
        return answer && !isPartialUpdate;
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        try {
            Optional<String> requestBody = readRequestBody(currentContext);
            if (!requestBody.isPresent()) {
                log.warn("Requested document content is empty ({})", currentContext.getRequest().getRequestURI());
                setAuthorizationRan(true);
                return null;
            }
            JsonNode document = objectMapper.readTree(requestBody.get());
            PreAuthorizationResult authResult = getPreAuthorizationResult();
            String luceneQuery = authResult.getLuceneQuery().toString();
            if (!documentEvaluator.matches(document, luceneQuery)) {
                log.info("Authorization failed. The request body does not match query: {}", luceneQuery);
                throw new UnauthorizedException("The user is unauthorized to create or update this document.");
            }
        } catch (IOException | ParseException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        setAuthorizationRan(true);
        return null;
    }
}
