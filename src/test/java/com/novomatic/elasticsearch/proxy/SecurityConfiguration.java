package com.novomatic.elasticsearch.proxy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.novomatic.elasticsearch.proxy.config.KeycloakAdapterProperties;
import com.novomatic.elasticsearch.proxy.utils.TokenAuthenticationService;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextFactoryBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

@TestConfiguration
class SecurityConfiguration {

    @Bean
    @Primary
    public AdapterDeploymentContext testAdapterDeploymentContext(TokenAuthenticationService tokenAuthenticationService, KeycloakAdapterProperties keycloakAdapterProperties) throws Exception {
        keycloakAdapterProperties.setRealmKey(tokenAuthenticationService.getPublicKey());
        AdapterDeploymentContextFactoryBean factoryBean = new AdapterDeploymentContextFactoryBean(facade -> KeycloakDeploymentBuilder.build(keycloakAdapterProperties));
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public TokenAuthenticationService tokenAuthenticationService(RSAKey rsaKey) {
        return new TokenAuthenticationService(rsaKey);
    }

    @Bean
    public RSAKey rsaKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }
}