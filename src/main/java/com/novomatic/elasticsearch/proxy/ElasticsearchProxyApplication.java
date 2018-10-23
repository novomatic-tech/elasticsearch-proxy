package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AuthorizationProperties.class})
public class ElasticsearchProxyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchProxyApplication.class, args);
    }
}
