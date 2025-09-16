package com.agent.doc.generator.impl;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.agent.doc.generator.service.JiraService;

@Service
public class JiraServiceImpl implements JiraService {

    @Value("${jira.base.url}")
    private String jiraBaseUrl;

    @Value("${jira.api.token}")
    private String jiraToken;
    
    @Value("${confluence.user}")
    private String jiraUser; // mismo usuario para Jira

    private final RestTemplate restTemplate = new RestTemplate();

    public String getIssueContent(String issueId) {
        try {
            String url = jiraBaseUrl + "/rest/api/3/issue/" + issueId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + base64Credentials());
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map fields = (Map) response.getBody().get("fields");
            String summary = (String) fields.get("summary");

            Object descriptionObj = fields.get("description");
            String description = "";

            if (descriptionObj instanceof Map) {
                description = extractPlainTextFromADF((Map) descriptionObj);
            } else if (descriptionObj != null) {
                description = descriptionObj.toString();
            }

            return "Resumen: " + summary + "\n\nDescripción: " + description;

        } catch (Exception e) {
            return "Error al obtener Jira: " + e.getMessage();
        }
    }
    
    @SuppressWarnings("unchecked")
    private String extractPlainTextFromADF(Map<String, Object> adf) {
        StringBuilder sb = new StringBuilder();
        Object contentObj = adf.get("content");
        if (contentObj instanceof List) {
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;
            for (Map<String, Object> block : contentList) {
                Object innerContentObj = block.get("content");
                if (innerContentObj instanceof List) {
                    List<Map<String, Object>> innerContent = (List<Map<String, Object>>) innerContentObj;
                    for (Map<String, Object> node : innerContent) {
                        if ("text".equals(node.get("type"))) {
                            sb.append(node.get("text")).append(" ");
                        }
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    private String base64Credentials() {
        String auth = jiraUser + ":" + jiraToken;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }
}