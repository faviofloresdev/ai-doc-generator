package com.agent.doc.generator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.agent.doc.generator.dto.GeminiRequest;
import com.agent.doc.generator.util.DocumentationProperties;

@Service
public class GeminiService {

	private final WebClient webClient;	
	
	@Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location}")
    private String location;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    private String model;

    @Value("${gemini.api.key}")
    private String apiKey;
    
    private WebClient.Builder webClientBuilder;
    
    private final DocumentationProperties promptConfig;
    
    public GeminiService(WebClient.Builder webClientBuilder, DocumentationProperties promptConfig) {
    	this.promptConfig = promptConfig;
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public String generateMarkdown(String jiraContent, String gitChanges, String confluenceContent,
            String proceso, String complejidad, String menu, String autor,
            String huRelacionados) {
    	
    	String finalPrompt = promptConfig.getPrompt()
                .replace("{jiraContent}", jiraContent)
                .replace("{gitChanges}", gitChanges)
                .replace("{confluenceContent}", confluenceContent)
                .replace("{proceso}", proceso)
                .replace("{complejidad}", complejidad)
                .replace("{menu}", menu)
                .replace("{autor}", autor)
                .replace("{huRelacionados}", huRelacionados);

    	GeminiRequest request = new GeminiRequest(finalPrompt);
    	
    	String responseJson = webClient.post()
                .uri("/" + model + ":generateContent?key=" + apiKey) 
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    	 
    	try {
            // Extraer Markdown del JSON
            JSONObject json = new JSONObject(responseJson);
            JSONArray candidates = json.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String markdown = parts.getJSONObject(0).getString("text");
            return markdown;
        } catch (Exception e) {
            return "Error extrayendo Markdown: " + e.getMessage();
        }
    }
    
}