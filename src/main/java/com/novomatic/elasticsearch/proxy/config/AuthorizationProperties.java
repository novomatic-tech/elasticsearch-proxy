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

    /**
     * A rule defining a subset of users allowed to perform all actions on the Elasticsearch cluster.
     */
    private PrincipalConstraints admin = null;

    /**
     * A value indicating that Elasticsearch requests with auth schemes other than Bearer token (i.e. Basic Auth)
     * should be sent directly to the Elasticsearch cluster rather than being authorized in the proxy.
     */
    private boolean passThroughUnknownAuthorizationScheme = true;
}
