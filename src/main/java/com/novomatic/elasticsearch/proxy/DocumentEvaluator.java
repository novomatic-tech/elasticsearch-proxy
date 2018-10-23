package com.novomatic.elasticsearch.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.lucene.queryparser.classic.ParseException;

public interface DocumentEvaluator {
    boolean matches(JsonNode document, String luceneQuery) throws ParseException;
}
