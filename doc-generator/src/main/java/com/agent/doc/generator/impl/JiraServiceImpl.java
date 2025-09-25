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

            if (descriptionObj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> adfMap = (Map<String, Object>) descriptionObj;
                description = extractPlainTextFromADF(adfMap);
            } else if (descriptionObj != null) {
                description = descriptionObj.toString();
            }
            
            Object cf37Obj = fields.get("customfield_10037");
            String cf37 = "";
            
            if (cf37Obj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> adfMap = (Map<String, Object>) cf37Obj;
                cf37 = extractPlainTextFromADF(adfMap);
            } else if (cf37Obj != null) {
                description = cf37Obj.toString();
            }
            
            Object cf58Obj = fields.get("customfield_10058");
            String cf58 = "";
            
            if (cf58Obj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> adfMap = (Map<String, Object>) cf58Obj;
                cf58 = extractPlainTextFromADF(adfMap);
            } else if (cf58Obj != null) {
            	cf58 = cf58Obj.toString();
            }
            
            return "Resumen: " + summary
                    + "\n\nDescripción: " + description
                    + "\n\nCustomfield 10037: " + cf37
                    + "\nCustomfield 10058: " + cf58;

        } catch (Exception e) {
            return "Error al obtener Jira: " + e.getMessage();
        }
    }
    
    public String extractPlainTextFromADF(Map<String, Object> adf) {
        StringBuilder sb = new StringBuilder();

        // Si el root tiene "content" iteramos sobre sus hijos, si no, procesamos el objeto entero.
        Object topContent = adf.get("content");
        if (topContent instanceof List) {
            for (Object child : (List<?>) topContent) {
                extractNode(child, sb);
            }
        } else {
            extractNode(adf, sb);
        }

        // Normalizar espacios y saltos de línea
        String result = sb.toString()
                          .replaceAll("[ \\t]+", " ")
                          .replaceAll(" ?\\n ?", "\n")
                          .replaceAll("\\n{2,}", "\n\n")
                          .trim();
        return result;
    }

    @SuppressWarnings("unchecked")
    private void extractNode(Object nodeObj, StringBuilder sb) {
        if (nodeObj == null) return;

        // Si es una lista, recorrer cada elemento
        if (nodeObj instanceof List) {
            for (Object child : (List<?>) nodeObj) extractNode(child, sb);
            return;
        }

        if (!(nodeObj instanceof Map)) return;
        Map<String, Object> node = (Map<String, Object>) nodeObj;
        String type = (String) node.get("type");

        // Nodo de texto
        if ("text".equals(type)) {
            Object text = node.get("text");
            if (text != null) {
                sb.append(text).append(" ");
            }
            return;
        }

        // Párrafo / encabezado / blockquote -> procesar contenido y añadir salto de línea
        if ("paragraph".equals(type) || type != null && type.startsWith("heading") || "blockquote".equals(type)) {
            Object content = node.get("content");
            if (content != null) extractNode(content, sb);
            sb.append("\n");
            return;
        }

        // listItem -> procesar su contenido y añadir salto de línea
        if ("listItem".equals(type)) {
            Object content = node.get("content");
            if (content != null) extractNode(content, sb);
            sb.append("\n");
            return;
        }

        // orderedList / bulletList -> iterar sobre items y prefijarlos
        if ("orderedList".equals(type) || "bulletList".equals(type)) {
            List<?> items = (List<?>) node.get("content");
            if (items != null) {
                int index = 1;
                Object attrs = node.get("attrs");
                if (attrs instanceof Map) {
                    Object orderAttr = ((Map<?,?>) attrs).get("order");
                    if (orderAttr instanceof Number) index = ((Number) orderAttr).intValue();
                    else if (orderAttr instanceof String) {
                        try { index = Integer.parseInt((String) orderAttr); } catch (NumberFormatException ignored) {}
                    }
                }
                for (Object item : items) {
                    if ("orderedList".equals(type)) {
                        sb.append(index).append(". ");
                        extractNode(item, sb);
                        index++;
                    } else {
                        sb.append("- ");
                        extractNode(item, sb);
                    }
                }
            }
            return;
        }

        // Ignorar media/mediaSingle u otros nodos que no aportan texto directamente
        if ("media".equals(type) || "mediaSingle".equals(type)) {
            return;
        }

        // Fallback: si tiene "content", recorrerlo
        Object content = node.get("content");
        if (content != null) extractNode(content, sb);
    }

    private String base64Credentials() {
        String auth = jiraUser + ":" + jiraToken;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }
}