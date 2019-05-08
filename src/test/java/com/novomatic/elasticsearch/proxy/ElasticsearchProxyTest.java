package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.utils.AccessToken;
import com.novomatic.elasticsearch.proxy.utils.TokenAuthenticationService;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = {SecurityConfiguration.class})
public class ElasticsearchProxyTest {

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    @Test
    public void contextLoads() {

        AccessToken accessToken = new AccessToken.Builder()
                .issuer("http://localhost:8080/auth/realms/example")
                .type("Bearer")
                .subject("sample_subject")
                .resourceAccess("kibana", new AccessToken.Access("manage-kibana"))
                .build();

        String token = tokenAuthenticationService.serializeToken(accessToken);
    }
}
