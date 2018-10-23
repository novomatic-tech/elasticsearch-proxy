package com.novomatic.elasticsearch.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.springframework.util.ReflectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentEvaluatorImpl implements DocumentEvaluator {

    public static final String SOURCE_FIELD = "_source";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Pattern fieldPattern = Pattern.compile("([^ \"()]+?):");

    @Override
    public boolean matches(JsonNode document, String luceneQuery) throws ParseException {
        if (document.isMissingNode()) {
            return false;
        }
        if (luceneQuery == null || luceneQuery.equals("")) {
            return true;
        }
        Collection<String> fields = extractPossibleFields(luceneQuery);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        MemoryIndex index = buildIndex(analyzer, document, fields);
        QueryParser queryParser = new QueryParser(SOURCE_FIELD, analyzer);
        Query query = queryParser.parse(luceneQuery);
        float score = index.search(query);
        return score > 0.0f;
    }

    private Collection<String> extractPossibleFields(String luceneQuery) {
        Matcher matcher = fieldPattern.matcher(luceneQuery);
        Set<String> fields = new HashSet<>();
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        return fields;
    }

    private MemoryIndex buildIndex(Analyzer analyzer, JsonNode document, Collection<String> fields) {
        try {
            MemoryIndex index = new MemoryIndex();
            String source = OBJECT_MAPPER.writeValueAsString(document);
            index.addField(SOURCE_FIELD, source, analyzer);
            for (String field : fields) {
                String jsonPath = '/' + field.replace('.', '/');
                JsonNode node = document.at(jsonPath);
                if (!node.isMissingNode()) {
                    String value = node.textValue();
                    if (value == null) {
                        value = OBJECT_MAPPER.writeValueAsString(node);
                    }
                    index.addField(field, value, analyzer);
                }
            }
            return index;
        } catch (JsonProcessingException e) {
            ReflectionUtils.rethrowRuntimeException(e);
            return null;
        }
    }



}
