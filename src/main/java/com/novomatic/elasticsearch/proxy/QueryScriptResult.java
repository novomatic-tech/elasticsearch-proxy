package com.novomatic.elasticsearch.proxy;

import lombok.Data;

@Data
public class QueryScriptResult {
    private final String luceneQuery;
}
