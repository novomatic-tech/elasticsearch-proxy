package com.novomatic.elasticsearch.proxy.utils;

import java.util.Map;

public interface Token {

    Map<String, Object> getClaims();
}
