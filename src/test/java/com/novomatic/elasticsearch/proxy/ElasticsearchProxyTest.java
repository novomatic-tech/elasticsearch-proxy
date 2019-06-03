package com.novomatic.elasticsearch.proxy;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.novomatic.elasticsearch.proxy.utils.AccessToken;
import com.novomatic.elasticsearch.proxy.utils.TokenAuthenticationService;
import org.apache.commons.io.Charsets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Integration tests based on the application.yaml configuration file placed in test resources.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {SecurityConfiguration.class})
@AutoConfigureWireMock(port = 9201)
public class ElasticsearchProxyTest {

    private static final String KEYCLOAK_ISSUER = "http://localhost:8080/auth/realms/example";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void adminCanDoAnything() {
        // Arrange
        String indexPatternPath = "/.kibana/_doc/index-pattern:d3d7af60-4c81-11e8-b3d7-01146121b73d";
        stubFor(delete(indexPatternPath));
        AccessToken accessToken = tokenBuilder()
                .resourceAccess("kibana", new AccessToken.Access("manage-kibana")) //admin right
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(indexPatternPath, HttpMethod.DELETE, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deleteRequestedFor(urlEqualTo(indexPatternPath)));
    }

    @Test
    public void authenticatedUserCanReadIndexPatterns() {
        // Arrange
        stubFor(post("/.kibana/_search"));
        AccessToken accessToken = tokenBuilder().build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        String requestBody = readResource("classpath:/index-pattern-request.json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange("/.kibana/_search", HttpMethod.POST, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that query has been added to the request body
        verify(postRequestedFor(urlEqualTo("/.kibana/_search"))
                .withRequestBody(matchingJsonPath("$.query.bool.filter[1].bool.should[0].query_string.query", equalTo("(type:index-pattern OR type:config)"))));
    }

    @Test
    public void unauthenticatedUserCannotDoAnything() {
        // Arrange
        stubFor(post("/logs/_search"));

        // Act
        ResponseEntity<String> response = restTemplate.exchange("/logs/_search", HttpMethod.POST, HttpEntity.EMPTY, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(exactly(0), postRequestedFor(urlEqualTo("/logs/_search")));
    }

    @Test
    public void generateQueryBasedOnAccessToken() {
        // Arrange
        stubFor(post("/flights/_search"));
        AccessToken accessToken = tokenBuilder()
                .claim("countries", Arrays.asList("PL", "EN"))
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        String requestBody = readResource("classpath:/flights-request.json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange("/flights/_search", HttpMethod.POST, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that query has been added to the request body
        verify(postRequestedFor(urlEqualTo("/flights/_search"))
                .withRequestBody(matchingJsonPath("$.query.bool.filter[1].bool.should[0].query_string.query", equalTo("(OriginCountry:PL OR OriginCountry:EN)"))));
    }


    private AccessToken.Builder tokenBuilder() {
        return new AccessToken.Builder()
                .issuer(KEYCLOAK_ISSUER)
                .type("Bearer")
                .subject("sample_subject");
    }

    private String readResource(String location) {
        try {
            Path path = Paths.get(resourceLoader.getResource(location).getURI());
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
