package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public class SearchFilterTest {

    @Test
    public void test() throws IOException {
        ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
        ObjectNode query = OBJECT_MAPPER.createObjectNode();
        rootNode.set("query", query);
        ObjectNode bool = OBJECT_MAPPER.createObjectNode();
        query.set("bool", bool);
        ArrayNode filters = OBJECT_MAPPER.createArrayNode();
        bool.set("filter", filters);

        Optional<JsonNode> queryFromBody = Optional.of(OBJECT_MAPPER.readTree(
            "{\"query\":{\"bool\":{\"filter\":[{\"terms\":{\"type\":[\"dashboard\"]}}]}}}"));

//        Optional<JsonNode> queryFromBody = Optional.of(OBJECT_MAPPER.readTree(
//            "{\"query\":{\"term\":{\"type\":\"dashboard\"}}}"));

        if (queryFromBody.isPresent()) {
            JsonNode existingQuery = queryFromBody.get().path("query");
            if (existingQuery.isMissingNode()) {
                throw new IllegalArgumentException("Invalid query DSL");
            }
            JsonNode existingBoolQuery = existingQuery.path("bool");
            if (existingBoolQuery.isMissingNode()) {
                ObjectNode mustNode = OBJECT_MAPPER.createObjectNode();
                existingQuery.fields().forEachRemaining(field -> mustNode.set(field.getKey(), field.getValue()));
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

        System.out.println(query.toString());
    }

}