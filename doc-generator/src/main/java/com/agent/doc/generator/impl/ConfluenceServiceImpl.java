package com.agent.doc.generator.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.agent.doc.generator.service.ConfluenceService;

import reactor.util.retry.Retry;

@Service
public class ConfluenceServiceImpl implements ConfluenceService {

    @Value("${confluence.base.url}")
    private String baseUrl;

    @Value("${confluence.user}")
    private String user;
    
    @Value("${confluence.token}")
    private String token;
    
    private String token64;

    private final WebClient webClient = WebClient.create();
    
    @PostConstruct
    public void init() {
    	String auth = user + ":" + token;
    	token64 = Base64.getEncoder().encodeToString(auth.getBytes());
    }
    
    private String getAuthHeader() {
        return "Basic " + token64;
    }
    
    @Override
    public String getExistingContent(String pageId) {
    	try {
            Map response = webClient.get()
                    .uri(baseUrl + "/rest/api/content/{id}?expand=body.storage", pageId)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("body")) {
                Map body = (Map) response.get("body");
                if (body.containsKey("storage")) {
                    Map storage = (Map) body.get("storage");
                    return storage.get("value").toString(); // Esto es el contenido en storage format
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void publishPage(String pageId, String title, String markdownContent) {
        String url = baseUrl + "/rest/api/content/" + pageId;

        Map<String, Object> body = Map.of(
            "id", pageId,
            "type", "page",
            "title", title,
            "body", Map.of(
                "storage", Map.of(
                    "value", markdownContent,
                    "representation", "wiki"
                )
            )
        );

        webClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + token64)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}