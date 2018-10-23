package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationProperties;
import com.novomatic.elasticsearch.proxy.filter.*;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@EnableZuulProxy
@Configuration
public class ElasticsearchProxyConfig {

    @Value("classpath:keycloak.json")
    private Resource keycloakConfigFileResource;

    @Bean
    public AdapterDeploymentContext adapterDeploymentContext() throws Exception {
        AdapterDeploymentContextFactoryBean factoryBean = new AdapterDeploymentContextFactoryBean(keycloakConfigFileResource);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public NoDefaultEncodingFilter noDefaultEncodingFilter() {
        return new NoDefaultEncodingFilter();
    }

    @Bean
    public AuthenticationFilter authorizationFilter(AdapterDeploymentContext adapterDeploymentContext) {
        return new AuthenticationFilter(adapterDeploymentContext);
    }

    @Bean
    public QuerySubstitutionFilter querySubstitutionFilter() {
        return new QuerySubstitutionFilter();
    }

    @Bean
    public AuthorizationService authorizationService(AuthorizationProperties authorizationProperties) {
        return new AuthorizationServiceImpl(authorizationProperties);
    }

    @Bean
    public SingleGetFilter getFilter(DocumentEvaluator documentEvaluator) {
        return new SingleGetFilter(documentEvaluator);
    }

    @Bean
    public DocumentEvaluator documentEvaluator() {
        return new DocumentEvaluatorImpl();
    }

    @Bean
    public PreAuthorizationFilter preAuthorizationFilter(AuthorizationService authorizationService) {
        return new PreAuthorizationFilter(authorizationService);
    }

    @Bean
    public MultiGetFilter multiGetFilter(DocumentEvaluator documentEvaluator) {
        return new MultiGetFilter(documentEvaluator);
    }

    @Bean
    public MultiSearchFilter multiSearchFilter() {
        return new MultiSearchFilter();
    }
}
