package com.novomatic.elasticsearch.proxy;

import lombok.Data;

import java.util.Optional;

@Data
public class QueryScriptResult {
    private final Optional<String> luceneQuery;
}
