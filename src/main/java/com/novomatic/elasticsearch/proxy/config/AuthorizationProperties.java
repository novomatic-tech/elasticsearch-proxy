package com.novomatic.elasticsearch.proxy.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "elasticsearch.proxy.security")
public class AuthorizationProperties {
    /**
     * A collection of authorization rules allowing access to defined Elasticsearch resources.
     */
    private List<AuthorizationRule> allow = Collections.emptyList();
}
