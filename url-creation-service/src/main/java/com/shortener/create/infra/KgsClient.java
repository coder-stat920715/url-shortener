package com.shortener.create.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class KgsClient {

    private final RestClient http;

    public KgsClient(@Value("${shortener.kgs.base-url}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<String> fetchBatch(int size) {
        List<String> keys = http.get()
                .uri("/internal/keys/batch?size={size}", size)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});
        return keys == null ? List.of() : keys;
    }
}
