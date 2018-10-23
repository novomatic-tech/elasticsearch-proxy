package com.novomatic.elasticsearch.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentEvaluatorImplTest {

    private JsonNode document;

    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        document = objectMapper.readTree("{ \"id\": \"1234\", \"user\": { \"firstName\": \"John\", \"lastName\": \"Doe\" } }");
    }

    @Test
    public void shouldMatchNestedField() throws Exception {
        // given
        DocumentEvaluator documentEvaluator = new DocumentEvaluatorImpl();

        // when
        boolean matches = documentEvaluator.matches(document, "user.firstName:John");

        // then
        assertTrue(matches);
    }

    @Test
    public void shouldBeCaseInsensitive() throws Exception {
        // given
        DocumentEvaluator documentEvaluator = new DocumentEvaluatorImpl();

        // when
        boolean matches = documentEvaluator.matches(document, "user.firstName:john");

        // then
        assertTrue(matches);
    }

    @Test
    public void shouldMatchTopLevelField() throws Exception {
        // given
        DocumentEvaluator documentEvaluator = new DocumentEvaluatorImpl();

        // when
        boolean matches = documentEvaluator.matches(document, "id:\"1234\"");

        // then
        assertTrue(matches);
    }

    @Test
    public void shouldMatchComplexQuery() throws Exception {
        // given
        DocumentEvaluator documentEvaluator = new DocumentEvaluatorImpl();

        // when
        boolean matches = documentEvaluator.matches(document, "(id:123* AND user.lastName:Do*)");

        // then
        assertTrue(matches);
    }

    @Test
    public void shouldNotMatchComplexQuery() throws Exception {
        // given
        DocumentEvaluator documentEvaluator = new DocumentEvaluatorImpl();

        // when
        boolean matches = documentEvaluator.matches(document, "id:44*4 OR user.lastName:Smi*");

        // then
        assertFalse(matches);
    }
}