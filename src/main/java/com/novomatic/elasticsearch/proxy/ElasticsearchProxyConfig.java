package com.novomatic.elasticsearch.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novomatic.elasticsearch.proxy.config.AuthorizationProperties;
import com.novomatic.elasticsearch.proxy.config.KeycloakAdapterProperties;
import com.novomatic.elasticsearch.proxy.filter.*;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextFactoryBean;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableZuulProxy
@Configuration
public class ElasticsearchProxyConfig {

    @Bean
    public AdapterDeploymentContext adapterDeploymentContext(KeycloakAdapterProperties keycloakAdapterProperties) throws Exception {
        AdapterDeploymentContextFactoryBean factoryBean = new AdapterDeploymentContextFactoryBean(facade -> KeycloakDeploymentBuilder.build(keycloakAdapterProperties));
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public NoDefaultEncodingFilter noDefaultEncodingFilter() {
        return new NoDefaultEncodingFilter();
    }

    @Bean
    public AuthenticationFilter authorizationFilter(AdapterDeploymentContext adapterDeploymentContext, AuthorizationProperties authorizationProperties) {
        return new AuthenticationFilter(adapterDeploymentContext, authorizationProperties);
    }

    @Bean
    public QuerySubstitutionFilter querySubstitutionFilter() {
        return new QuerySubstitutionFilter();
    }

    @Bean
    public AuthorizationService authorizationService(AuthorizationProperties authorizationProperties, QueryScriptEvaluator queryScriptEvaluator) {
        return new AuthorizationServiceImpl(authorizationProperties, queryScriptEvaluator);
    }

    @Bean
    public GetDocumentFilter getFilter(ObjectMapper objectMapper, DocumentEvaluator documentEvaluator) {
        return new GetDocumentFilter(objectMapper, documentEvaluator);
    }

    @Bean
    public DocumentEvaluator documentEvaluator(ObjectMapper objectMapper) {
        return new DocumentEvaluatorImpl(objectMapper);
    }

    @Bean
    public PreAuthorizationFilter preAuthorizationFilter(AuthorizationService authorizationService) {
        return new PreAuthorizationFilter(authorizationService);
    }

    @Bean
    public MultiGetFilter multiGetFilter(ObjectMapper objectMapper, DocumentEvaluator documentEvaluator) {
        return new MultiGetFilter(objectMapper, documentEvaluator);
    }

    @Bean
    public MultiSearchFilter multiSearchFilter(AuthorizationService authorizationService) {
        return new MultiSearchFilter(authorizationService);
    }

    @Bean
    public WriteDocumentFilter writeDocumentFilter(ObjectMapper objectMapper, DocumentEvaluator documentEvaluator) {
        return new WriteDocumentFilter(objectMapper, documentEvaluator);
    }
    @Bean
    public DeleteDocumentFilter deleteDocumentFilter(DocumentEvaluator documentEvaluator) {
        return new DeleteDocumentFilter(documentEvaluator);
    }
    @Bean
    public ReadPostAuthorizationFilter readPostAuthorizationFilter() {
        return new ReadPostAuthorizationFilter();
    }
    @Bean
    public WritePostAuthorizationFilter writePostAuthorizationFilter() {
        return new WritePostAuthorizationFilter();
    }

    @Bean
    public CheckIndexFilter checkIndexFilter() {
        return new CheckIndexFilter();
    }

    @Bean
    public WrapErrorFilter wrapErrorFilter() {
        return new WrapErrorFilter();
    }

    @Bean
    public QueryScriptEvaluator queryScriptEvaluator() {
        return new GroovyQueryScriptEvaluator();
    }
}
