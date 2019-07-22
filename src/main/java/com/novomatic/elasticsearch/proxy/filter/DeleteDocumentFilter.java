package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import com.novomatic.elasticsearch.proxy.PreAuthorizationResult;
import com.novomatic.elasticsearch.proxy.DocumentEvaluator;
import com.novomatic.elasticsearch.proxy.ElasticsearchRequest;
import com.novomatic.elasticsearch.proxy.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.novomatic.elasticsearch.proxy.DocumentEvaluatorImpl.SOURCE_FIELD;

@Slf4j
public class DeleteDocumentFilter extends ElasticsearchApiFilter {

    private final DocumentEvaluator documentEvaluator;
    private final RestTemplate restTemplate;

    public DeleteDocumentFilter(DocumentEvaluator documentEvaluator) {
        this.documentEvaluator = documentEvaluator;
        restTemplate = new RestTemplate();
    }

    @Override
    public boolean shouldFilter() {
        if (isPassThrough() || !isElasticsearchRequest()) {
            return false;
        }
        ElasticsearchRequest request = getElasticsearchRequest();
        return request.usesMethod(HttpMethod.DELETE) &&
                request.hasExplicitIndices() &&
                request.hasTypes() &&
                request.hasResourceId() &&
                !request.hasOperation();
    }

    @Override
    public Object run() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = getElasticsearchRequest();
        JsonNode document;
        String requestUrl = getTargetRequestUrl(currentContext);
        try {
            document = restTemplate.getForObject(requestUrl, JsonNode.class);
            JsonNode sourceNode = document.path(SOURCE_FIELD);
            PreAuthorizationResult authResult = getPreAuthorizationResult();
            if (!documentEvaluator.matches(sourceNode, authResult.getLuceneQuery())) {
                log.info("Authorization failed. The document at {} does not match query: {}",
                        request.getRequestURI(), authResult.getLuceneQuery());
                throw new UnauthorizedException("The user is unauthorized to delete this document.");
            }
            JsonNode versionNode = document.path("_version");
            if (!versionNode.isMissingNode()) {
                Map<String, List<String>> map = HTTPRequestUtils.getInstance().getQueryParams();
                if (map == null) {
                    map = new LinkedHashMap<>();
                }
                map.put("version", Collections.singletonList("" + versionNode.longValue()));
                currentContext.setRequestQueryParams(map);
            }
        } catch (HttpStatusCodeException ex) {
            String message = String.format("The GET %s request returned the %s status code.", requestUrl, ex.getStatusCode());
            log.warn(message, ex);
            currentContext.setSendZuulResponse(false);
            if (ex.getResponseHeaders() != null) {
                ex.getResponseHeaders().forEach((key, value) -> {
                    currentContext.addZuulResponseHeader(key, value.isEmpty() ? "" : value.get(0));
                });
            }
            currentContext.setResponseBody(ex.getResponseBodyAsString());
            currentContext.setResponseStatusCode(ex.getRawStatusCode());
        } catch (ParseException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        setAuthorizationRan(true);
        return null;
    }

    private String getTargetRequestUrl(RequestContext currentContext) {
        URL routeHost = currentContext.getRouteHost();
        HttpServletRequest request = currentContext.getRequest();
        try {
            String requestUri = java.net.URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8.name());
            return new URL(routeHost, requestUri).toString();
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            ReflectionUtils.rethrowRuntimeException(e);
            return null;
        }
    }
}
