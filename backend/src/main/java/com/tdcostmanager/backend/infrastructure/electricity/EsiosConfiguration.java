package com.tdcostmanager.backend.infrastructure.electricity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class EsiosConfiguration {

    @Bean
    public RestClient esiosRestClient(EsiosProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());

        return builder
                .baseUrl(properties.getUrl())
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json; application/vnd.esios-api-v1+json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
