package com.novomatic.elasticsearch.proxy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.Charsets;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.novomatic.elasticsearch.proxy.utils.AccessToken;
import com.novomatic.elasticsearch.proxy.utils.TokenAuthenticationService;

/**
 * Integration tests based on the application.yaml configuration file placed in test resources.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = { SecurityConfiguration.class })
@AutoConfigureWireMock(port = 9201)
public class ElasticsearchProxyTest {

    private static final String KEYCLOAK_ISSUER = "http://localhost:9201/auth/realms/example";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    @Autowired
    private ResourceLoader resourceLoader;

    @BeforeEach
    public void setUp() {
        String openidConfig = ""
                + "{\"issuer\":\"http://localhost:9201/auth/realms/example\",\"authorization_endpoint\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/auth\","
                + "\"token_endpoint\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/token\","
                + "\"token_introspection_endpoint\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/token/introspect\","
                + "\"userinfo_endpoint\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/userinfo\","
                + "\"end_session_endpoint\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/logout\","
                + "\"jwks_uri\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/certs\","
                + "\"check_session_iframe\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/login-status-iframe.html\",\"grant_types_supported\":[\"authorization_code\","
                + "\"implicit\",\"refresh_token\",\"password\",\"client_credentials\"],\"response_types_supported\":[\"code\",\"none\",\"id_token\",\"token\",\"id_token token\",\"code "
                + "id_token\",\"code token\",\"code id_token token\"],\"subject_types_supported\":[\"public\",\"pairwise\"],\"id_token_signing_alg_values_supported\":[\"PS384\",\"ES384\","
                + "\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"id_token_encryption_alg_values_supported\":[\"RSA-OAEP\",\"RSA1_5\"],"
                + "\"id_token_encryption_enc_values_supported\":[\"A128GCM\",\"A128CBC-HS256\"],\"userinfo_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\","
                + "\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"request_object_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\","
                + "\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],"
                + "\"registration_endpoint\":\"http://localhost:9201/auth/realms/example/clients-registrations/openid-connect\",\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\","
                + "\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"token_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\","
                + "\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"claims_supported\":[\"aud\",\"sub\",\"iss\",\"auth_time\",\"name\","
                + "\"given_name\",\"family_name\",\"preferred_username\",\"email\",\"acr\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":false,"
                + "\"scopes_supported\":[\"openid\",\"address\",\"email\",\"microprofile-jwt\",\"offline_access\",\"phone\",\"profile\",\"roles\",\"web-origins\"],"
                + "\"request_parameter_supported\":true,\"request_uri_parameter_supported\":true,\"code_challenge_methods_supported\":[\"plain\",\"S256\"],"
                + "\"tls_client_certificate_bound_access_tokens\":true,\"introspection_endpoint\":\"http://localhost:9201/auth/realms/example/protocol/openid-connect/token/introspect\"}";

        stubFor(WireMock.get("/auth/realms/example/.well-known/openid-configuration")
                        .willReturn(aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(openidConfig)
                        )
        );
    }

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
