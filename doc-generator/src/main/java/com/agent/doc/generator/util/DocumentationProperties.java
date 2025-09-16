package com.agent.doc.generator.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "documentation")
public class DocumentationProperties {

	private String prompt;

	public String getPrompt() {
	    return prompt;
	}
	
	public void setPrompt(String prompt) {
	    this.prompt = prompt;
	}
}