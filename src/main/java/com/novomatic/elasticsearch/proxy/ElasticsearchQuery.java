package com.novomatic.elasticsearch.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.novomatic.elasticsearch.proxy.filter.JsonNodeUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class ElasticsearchQuery {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JsonNode rootNode;
    private final JsonNode wrapperNode;

    private ElasticsearchQuery(JsonNode wrapperNode) {
        this.wrapperNode = wrapperNode;
        JsonNode queryNode = wrapperNode.path("query");
        this.rootNode = queryNode.isMissingNode() ? null : queryNode;
    }

    private ElasticsearchQuery() {
        this.wrapperNode = OBJECT_MAPPER.createObjectNode();
        this.rootNode = null;
    }

    public static ElasticsearchQuery empty() {
        return new ElasticsearchQuery();
    }

    public static ElasticsearchQuery fromJson(String json) throws IOException {
        if (json == null || json.equals("")) {
            return ElasticsearchQuery.empty();
        }
        JsonNode wrapperNode = OBJECT_MAPPER.readTree(json);
        return new ElasticsearchQuery(wrapperNode);
    }

    public static ElasticsearchQuery allowedIndices(Set<String> indices) {
        if (indices.isEmpty()) {
            return ElasticsearchQuery.empty();
        }
        List<String> quotedIndices = indices.stream().map(index -> "\"" + index + "\"").collect(Collectors.toList());
        String indicesArrayString = String.join(",", quotedIndices);
        try {
            JsonNode wrapperNode = OBJECT_MAPPER.readTree("{\"query\":{\"terms\":{\"_index\":[" + indicesArrayString + "]}}}");
            return new ElasticsearchQuery(wrapperNode);
        } catch (IOException e) {
            ReflectionUtils.rethrowRuntimeException(e);
            return null;
        }
    }

    public static ElasticsearchQuery fromLuceneQuery(String queryString) {
        if (queryString == null || queryString.equals("")) {
            return ElasticsearchQuery.empty();
        }
        ObjectNode wrapperNode = OBJECT_MAPPER.createObjectNode();
        ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        ObjectNode query = OBJECT_MAPPER.createObjectNode();
        query.set("query", query.textNode(queryString));
        rootNode.set("query_string", query);
        wrapperNode.set("query", rootNode);
        return new ElasticsearchQuery(wrapperNode);
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
     * Finds a nested JSON node in the query JSON.
     * @param property The JSON property name (i.e. query.terms).
     */
    public JsonNode getProperty(String property) {
        return JsonNodeUtils.findProperty(wrapperNode, property);
    }

    /**
     * Checks whether this query aggregates indices.
     * Is crucial NOT to concat the indices aggregation query with authorization queries such as:
     * <pre>
     * {
     *   "aggs": {
     *     "indices": {
     *       "terms": {
     *         "field": "_index",
     *         "size": 200
     *       }
     *     }
     *   }
     * }
     * </pre>
     * @return
     */
    public boolean isIndexAggregation() {
        JsonNode aggregations = wrapperNode.path("aggs");
        if (aggregations.isMissingNode()) {
            return false;
        }
        ImmutableList<Map.Entry<String, JsonNode>> properties = ImmutableList.copyOf(aggregations.fields());
        for (Map.Entry<String, JsonNode> property : properties) {
            JsonNode propertyNode = property.getValue();
            if (Iterators.size(propertyNode.fields()) != 1) {
                return false; // There is more than "terms" in the field
            }
            JsonNode termsFieldNode = JsonNodeUtils.findProperty(propertyNode, "terms.field");
            if (termsFieldNode.isMissingNode() || !termsFieldNode.textValue().equals("_index")) {
                return false;
            }
        }
        return true;
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
