package com.novomatic.elasticsearch.proxy;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import com.novomatic.elasticsearch.proxy.config.ResourceAction;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_ENTITY_KEY;

public class ElasticsearchRequest extends HttpServletRequestWrapper {

    private final Principal principal;
    private Set<String> indices = Collections.emptySet();
    private Set<String> types = Collections.emptySet();
    private String operation;
    private boolean allIndices = false;
    private boolean allTypes = false;
    private String resourceId;
    private byte[] body;

    public ElasticsearchRequest(HttpServletRequest request, Principal principal) {
        super(request);
        parseRequest();
        this.principal = principal;
    }

    private void parseRequest() {
        String[] tokens = getRequestURI().substring(1).split("/");
        parseIndices(tokens);
        parseTypes(tokens);
        parseOperation(tokens);
    }

    private void parseIndices(String[] tokens) {
        if (tokens.length > 0) {
            indices = parseIdentifiers(tokens[0]);
            allIndices = isWildcardMatchingAll(tokens[0]) || indices.isEmpty();
        }
    }

    private void parseTypes(String[] tokens) {
        if (tokens.length > 1) {
            types = parseIdentifiers(tokens[1]);
            allTypes = isWildcardMatchingAll(tokens[1]);
        }
    }

    private void parseOperation(String[] tokens) {
        if (tokens.length > 0) {
            String possibleAction = tokens[tokens.length - 1];
            if (possibleAction.startsWith("_")) {
                operation = possibleAction;
            } else {
                resourceId = possibleAction;
            }
        }
    }

    public Set<String> getIndices() {
        return indices;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public boolean hasTypes() {
        return allTypes || hasExplicitTypes();
    }

    public boolean hasExplicitTypes() {
        return !types.isEmpty();
    }

    public boolean hasIndices() {
        return allIndices || hasExplicitIndices();
    }

    public boolean hasExplicitIndices() {
        return !indices.isEmpty();
    }

    public boolean hasOperation() {
        return operation != null;
    }

    public boolean isMultiGet() {
        return "_mget".equals(operation);
    }

    public boolean isSearch() {
        return "_search".equals(operation);
    }

    public boolean isCount() {
        return "_count".equals(operation);
    }

    public ResourceAction deduceResourceAction() {
        if (getMethod().equalsIgnoreCase("GET") || getMethod().equalsIgnoreCase("HEAD")) {
            return ResourceAction.READ;
        }
        if (getMethod().equalsIgnoreCase("POST") &&
                (isSearch() || isCount() || isMultiGet() || isMultiSearch())) {
            return ResourceAction.READ;
        }
        return ResourceAction.WRITE;
    }

    public boolean isMultiSearch() {
        return "_msearch".equals(operation);
    }

    public boolean isDeleteByQuery() {
        return "_delete_by_query".equals(operation);
    }

    public boolean isUpdateByQuery() {
        return "_update_by_query".equals(operation);
    }

    public boolean isBulk() {
        return "_bulk".equalsIgnoreCase(operation);
    }

    public boolean hasResourceId() {
        return resourceId != null;
    }

    private static Set<String> parseIdentifiers(String urlPart) {
        return Stream.of(urlPart.split(","))
                .filter(index -> !index.startsWith("_"))
                .collect(Collectors.toSet());
    }

    public void setBody(String bodyString) {
        // TODO: refactor setting request body
        this.body = bodyString.getBytes(StandardCharsets.UTF_8);
        RequestContext.getCurrentContext().set(REQUEST_ENTITY_KEY, new ByteArrayInputStream(this.body));
    }

    @Override
    public int getContentLength() {
        return hasOverwrittenBody()
            ? body.length
            : super.getContentLength();
    }
    @Override
    public long getContentLengthLong() {
        return hasOverwrittenBody()
                ? body.length
                : super.getContentLengthLong();
    }
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return hasOverwrittenBody()
                ? new ServletInputStreamWrapper(body)
                : super.getInputStream();
    }

    private boolean hasOverwrittenBody() {
        return body != null;
    }

    private static boolean isWildcardMatchingAll(String urlPath) {
        return urlPath.equals("_all");
    }
}
