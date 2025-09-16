package com.agent.doc.generator.dto;

import java.util.List;
import java.util.Map;

public class GeminiRequest {
    private List<Map<String, Object>> contents;

    public GeminiRequest(String text) {
        this.contents = List.of(
                Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", text))
                )
        );
    }

    public List<Map<String, Object>> getContents() {
        return contents;
    }

    public void setContents(List<Map<String, Object>> contents) {
        this.contents = contents;
    }
}
