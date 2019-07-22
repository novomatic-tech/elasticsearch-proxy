package com.novomatic.elasticsearch.proxy;

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

import static com.github.tomakehurst.wiremock.client.WireMock.*;

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

    /**
     * Admin can do anything
     * Endpoint: POST /{index}/_search
     */
    @Test
    public void shouldProtectIndices() {
        // Arrange
        stubFor(post("/orders/_search"));
        AccessToken accessToken = tokenBuilder()
                .resourceAccess("kibana", new AccessToken.Access("manage-kibana")) //admin right
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/orders/_search", HttpMethod.POST, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(postRequestedFor(urlEqualTo("/orders/_search")));
    }

    /**
     * Authenticated user can read index patterns
     * Endpoint: POST /{index}/_search
     */
    @Test
    public void shouldProtectIndexPatterns() {
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

    /**
     * Unauthenticated user cannot do anything
     * Endpoint: POST /{index}/_search
     */
    @Test
    public void shouldAuthenticateOperations() {
        // Arrange
        stubFor(post("/logs/_search"));

        // Act
        ResponseEntity<String> response = restTemplate.exchange("/logs/_search", HttpMethod.POST, HttpEntity.EMPTY, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(exactly(0), postRequestedFor(urlEqualTo("/logs/_search")));
    }

    /**
     * Only dashboard owner can delete dashboard
     * Endpoint: DELETE /{index}/{type}/{id}
     */
    @Test
    public void shouldProtectDeleteOperation() {
        // Arrange
        String dashboardPath = "/.kibana/_doc/dashboard:7adfa750-4c81-11e8-b3d7-01146121b73d";
        String dashboardPathWithVersion = dashboardPath + "?version=1";
        String username = "john";
        stubFor(delete(dashboardPathWithVersion));
        stubFor(get(dashboardPath).willReturn(okJson(readResource("classpath:/dashboard-response.json"))));

        AccessToken accessToken = tokenBuilder()
                .resourceAccess("kibana", new AccessToken.Access("manage-dashboards"))
                .preferredUsername(username)
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(dashboardPath, HttpMethod.DELETE, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(getRequestedFor(urlEqualTo(dashboardPath)));
        verify(deleteRequestedFor(urlEqualTo(dashboardPathWithVersion)));
    }

    /**
     * Generate es query based on countries claim
     * Endpoint: POST /{index}/_search
     */
    @Test
    public void shouldProtectSearchOperation() {
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

    /**
     * Generate es query based on countries claim for multi search endpoint
     * Endpoint: POST /_msearch
     */
    @Test
    public void shouldProtectMultiSearchOperation() {
        // Arrange
        stubFor(post("/_msearch"));
        AccessToken accessToken = tokenBuilder()
                .claim("countries", Arrays.asList("PL", "EN"))
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        String requestBody = readResource("classpath:/multi-search-request.json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange("/_msearch", HttpMethod.POST, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that query has been added to the request body
        verify(postRequestedFor(urlEqualTo("/_msearch"))
                .withRequestBody(equalTo(readResource("classpath:/multi-search-expected-request.json"))));
    }

    /**
     * Generate es query based on countries claim for multi search endpoint when index defined
     * Endpoint: POST /{index}/_msearch
     */
    @Test
    public void shouldProtectMultiSearchOperationWhenIndexDefined() {
        // Arrange
        stubFor(post("/flights/_msearch"));
        AccessToken accessToken = tokenBuilder()
                .claim("countries", Arrays.asList("PL", "EN"))
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        String requestBody = readResource("classpath:/multi-search-request.json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange("/flights/_msearch", HttpMethod.POST, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that query has been added to the request body
        verify(postRequestedFor(urlEqualTo("/flights/_msearch"))
                .withRequestBody(equalTo(readResource("classpath:/multi-search-index-expected-request.json"))));
    }

    /**
     * User without countries claim cannot perform multi get
     * Endpoint: POST /{index}/_mget
     */
    @Test
    public void shouldProtectMultiGetOperation() {
        // Arrange
        String documentPath = "/flights/_mget";
        stubFor(post(documentPath).willReturn(okJson(readResource("classpath:/multi-get-response.json"))));
        AccessToken accessToken = tokenBuilder().build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        String requestBody = readResource("classpath:/multi-get-request.json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(documentPath, HttpMethod.POST, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(postRequestedFor(urlEqualTo(documentPath)));
    }

    /**
     * User without countries claim cannot get document
     * Endpoint: GET /{index}/{type}/{id}
     */
    @Test
    public void shouldProtectGetOperation() {
        // Arrange
        String documentPath = "/flights/_doc/FiiuQGsBjX0gYr95Aij3";
        stubFor(get(documentPath).willReturn(okJson(readResource("classpath:/get-flight-response.json"))));
        AccessToken accessToken = tokenBuilder().build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(documentPath, HttpMethod.GET, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(getRequestedFor(urlEqualTo(documentPath)));
    }

    /**
     * Endpoint: PUT /{index}/{type}/{id}
     */
    @Test
    public void shouldProtectWriteOperation() {
        // Arrange
        String documentPath = "/flights/_doc/ccbkaWsBuCIAHQsbxiqH";
        stubFor(put(documentPath).willReturn(okJson(readResource("classpath:/write-response.json"))));
        AccessToken accessToken = tokenBuilder()
                .resourceAccess("kibana", new AccessToken.Access("manage-flights"))
                .build();
        String bearerToken = tokenAuthenticationService.serializeToken(accessToken);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        String requestBody = readResource("classpath:/write-request.json");
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(documentPath, HttpMethod.PUT, entity, String.class);

        // Assert
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(putRequestedFor(urlEqualTo(documentPath)));
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
