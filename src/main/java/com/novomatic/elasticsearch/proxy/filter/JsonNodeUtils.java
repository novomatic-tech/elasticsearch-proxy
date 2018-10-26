package com.novomatic.elasticsearch.proxy.filter;

import com.fasterxml.jackson.databind.JsonNode;

public final class JsonNodeUtils {

    private JsonNodeUtils() {
    }

    public static JsonNode findProperty(JsonNode node, String path) {
        String jsonPath = '/' + path.replace('.', '/');
        return node.at(jsonPath);
    }
}
