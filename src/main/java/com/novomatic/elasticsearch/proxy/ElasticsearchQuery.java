package com.novomatic.elasticsearch.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ElasticsearchQuery {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JsonNode rootNode;
    private final JsonNode wrapperNode;

    private ElasticsearchQuery(JsonNode rootNode) {
        JsonNode queryNode = rootNode.path("query");
        if (queryNode.isMissingNode()) {
            this.rootNode = rootNode;
            wrapperNode = OBJECT_MAPPER.createObjectNode();
            ((ObjectNode) this.wrapperNode).set("query", rootNode);
        } else {
            this.rootNode = queryNode;
            wrapperNode = rootNode;
        }
    }

    private ElasticsearchQuery() {
        this.rootNode = null;
        this.wrapperNode = OBJECT_MAPPER.createObjectNode();
    }

    public static ElasticsearchQuery empty() {
        return new ElasticsearchQuery();
    }

    public static ElasticsearchQuery fromJson(String json) throws IOException {
        JsonNode rootNode = OBJECT_MAPPER.readTree(json);
        return new ElasticsearchQuery(rootNode);
    }

    public static ElasticsearchQuery fromLuceneQuery(String queryString) {
        if (queryString == null || queryString.equals("")) {
            return ElasticsearchQuery.empty();
        }
        ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        ObjectNode query = OBJECT_MAPPER.createObjectNode();
        query.set("query", query.textNode(queryString));
        rootNode.set("query_string", query);
        return new ElasticsearchQuery(rootNode);
    }

    public boolean isEmpty() {
        return rootNode == null;
    }

    public ElasticsearchQuery and(ElasticsearchQuery otherQuery) {
        if (otherQuery.isEmpty()) {
            return this;
        }
        ObjectNode newRootNode = OBJECT_MAPPER.createObjectNode();
        ObjectNode bool = OBJECT_MAPPER.createObjectNode();
        newRootNode.set("bool", bool);
        ArrayNode filters = OBJECT_MAPPER.createArrayNode();
        bool.set("filter", filters);
        copyExistingQueries(bool, filters);
        requireAtLeastOne(filters, Collections.singleton(otherQuery));
        ObjectNode newWrapperNode = OBJECT_MAPPER.createObjectNode();
        newWrapperNode.set("query", newRootNode);
        wrapperNode.fields().forEachRemaining(field -> {
            if (!field.getKey().equals("query")) {
                newWrapperNode.set(field.getKey(), field.getValue());
            }
        });
        return new ElasticsearchQuery(newWrapperNode);
    }

    private void copyExistingQueries(ObjectNode bool, ArrayNode filters) {
        if (isEmpty()) {
            return;
        }
        JsonNode existingBoolQuery = rootNode.path("bool");
        if (existingBoolQuery.isMissingNode()) {
            ObjectNode mustNode = OBJECT_MAPPER.createObjectNode();
            rootNode.fields().forEachRemaining(field -> mustNode.set(field.getKey(), field.getValue()));
            bool.set("must", mustNode);
        } else {
            existingBoolQuery.fields().forEachRemaining(field -> {
                if (field.getKey().equals("filter")) {
                    if (field.getValue() instanceof ArrayNode) {
                        filters.addAll((ArrayNode)field.getValue());
                    } else {
                        filters.add(field.getValue());
                    }
                } else {
                    bool.set(field.getKey(), field.getValue());
                }
            });
        }
    }

    private void requireAtLeastOne(ArrayNode filters, Collection<ElasticsearchQuery> elasticsearchQueries) {
        ObjectNode boolNode = OBJECT_MAPPER.createObjectNode();
        ArrayNode shouldNode = OBJECT_MAPPER.createArrayNode();
        boolNode.set("should", shouldNode);
        List<JsonNode> children = elasticsearchQueries.stream()
                .map(q -> q.rootNode)
                .collect(Collectors.toList());
        shouldNode.addAll(children);
        ObjectNode combinedNode = OBJECT_MAPPER.createObjectNode();
        combinedNode.set("bool", boolNode);
        filters.add(combinedNode);
    }

    /**
     * Generates the value of the "query" property.
     */
    public Optional<String> asInnerJson() {
        if (isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(rootNode));
        } catch (JsonProcessingException e) {
            ReflectionUtils.rethrowRuntimeException(e);
            return Optional.empty();
        }
    }

    /**
     * Generates a JSON with a "query" property at root level wrapping
     * the inner JSON.
     */
    public String asOuterJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(wrapperNode);
        } catch (JsonProcessingException e) {
            ReflectionUtils.rethrowRuntimeException(e);
            return "{}";
        }
    }

    @Override
    public String toString() {
        return asOuterJson();
    }
}
