package com.novomatic.elasticsearch.proxy.config;

import lombok.NoArgsConstructor;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NoArgsConstructor
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakAdapterProperties extends AdapterConfig {
}
