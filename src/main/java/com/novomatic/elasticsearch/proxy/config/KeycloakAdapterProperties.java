package com.novomatic.elasticsearch.proxy.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakAdapterProperties extends AdapterConfig {
}
