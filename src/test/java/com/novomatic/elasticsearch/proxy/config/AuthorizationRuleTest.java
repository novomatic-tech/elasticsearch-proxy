package com.novomatic.elasticsearch.proxy.config;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class AuthorizationRuleTest {

    @Test
    public void shouldMatchSubsetIndex() {
        // given
        AuthorizationRule rule = new AuthorizationRule();
        rule.getResources().setIndices(Collections.singleton("logstash-*"));

        // when
        boolean applicableForIndices = rule.isApplicableForIndices(Collections.singletonList("logstash-2012*"));

        // then
        assertTrue(applicableForIndices);
    }

    @Test
    public void shouldMatchSupersetIndex() {
        // given
        AuthorizationRule rule = new AuthorizationRule();
        rule.getResources().setIndices(Collections.singleton("logstash-*"));

        // when
        boolean applicableForIndices = rule.isApplicableForIndices(Collections.singletonList("logsta*"));

        // then
        assertTrue(applicableForIndices);
    }

    @Test
    public void shouldNotMatchWildcardedIndex() {
        // given
        AuthorizationRule rule = new AuthorizationRule();
        rule.getResources().setIndices(Collections.singleton("logstash-*"));

        // when
        boolean applicableForIndices = rule.isApplicableForIndices(Collections.singletonList("logsta-*"));

        // then
        assertFalse(applicableForIndices);
    }

    @Test
    public void shouldNotMatchExactIndex() {
        // given
        AuthorizationRule rule = new AuthorizationRule();
        rule.getResources().setIndices(Collections.singleton("logstash"));

        // when
        boolean applicableForIndices = rule.isApplicableForIndices(Collections.singletonList("logstash1*"));

        // then
        assertFalse(applicableForIndices);
    }

    @Test
    public void shouldAlwaysMatchEmptyIndices() {
        // given
        AuthorizationRule rule = new AuthorizationRule();
        rule.getResources().setIndices(Collections.singleton("logstash-*"));

        // when
        boolean applicableForIndices = rule.isApplicableForIndices(Collections.emptySet());

        // then
        assertTrue(applicableForIndices);
    }
}