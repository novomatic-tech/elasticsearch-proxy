package com.novomatic.elasticsearch.proxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.novomatic.elasticsearch.proxy.utils.AccessToken;
import com.novomatic.elasticsearch.proxy.utils.TokenAuthenticationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = {SecurityConfiguration.class})
public class ElasticsearchProxyTest {

    private static final String KEYCLOAK_ISSUER = "http://localhost:8080/auth/realms/example";
    @Rule
    public WireMockRule elasticsearchService = new WireMockRule(options().port(9201));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    @Before
    public void configure() {
        elasticsearchService.stubFor(get("/flights/_search"));
    }

    @Test
    public void shouldAllowAdminDoAnything() {
        AccessToken accessToken = adminToken();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String>  response = restTemplate.exchange("/flights/_search", HttpMethod.GET, entity, String.class);
        verify(getRequestedFor(urlEqualTo("/flights/_search")));
    }

    private AccessToken adminToken() {
        return new AccessToken.Builder()
                .issuer(KEYCLOAK_ISSUER)
                .type("Bearer")
                .subject("sample_subject")
                .resourceAccess("kibana", new AccessToken.Access("manage-kibana"))
                .build();
    }

    private AccessToken advancedToken() {
        return new AccessToken.Builder()
                .issuer(KEYCLOAK_ISSUER)
                .type("Bearer")
                .subject("sample_subject")
                .resourceAccess("kibana", new AccessToken.Access("view-dashboards"))
                .claim("countries", Arrays.asList("PL", "EN"))
                .build();
    }

    private AccessToken userToken() {
        return new AccessToken.Builder()
                .issuer(KEYCLOAK_ISSUER)
                .type("Bearer")
                .subject("sample_subject")
                .resourceAccess("kibana", new AccessToken.Access("view-dashboards"))
                .build();
    }
}
