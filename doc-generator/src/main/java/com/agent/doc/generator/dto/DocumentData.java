package com.agent.doc.generator.dto;

import java.util.List;

public class DocumentData{
	
	private List<String> jiraStories;
    private List<String> gitChanges;
    private String existingConfluenceContent;
    private String generatedMarkdown;
    private String confluencePageId;

    // Constructor vacío
    public DocumentData() {}

    // Constructor completo
    public DocumentData(List<String> jiraStories, List<String> gitChanges, String existingConfluenceContent,
                        String generatedMarkdown, String confluencePageId) {
        this.jiraStories = jiraStories;
        this.gitChanges = gitChanges;
        this.existingConfluenceContent = existingConfluenceContent;
        this.generatedMarkdown = generatedMarkdown;
        this.confluencePageId = confluencePageId;
    }

    // Getters y Setters
    public List<String> getJiraStories() { return jiraStories; }
    public void setJiraStories(List<String> jiraStories) { this.jiraStories = jiraStories; }

    public List<String> getGitChanges() { return gitChanges; }
    public void setGitChanges(List<String> gitChanges) { this.gitChanges = gitChanges; }

    public String getExistingConfluenceContent() { return existingConfluenceContent; }
    public void setExistingConfluenceContent(String existingConfluenceContent) { this.existingConfluenceContent = existingConfluenceContent; }

    public String getGeneratedMarkdown() { return generatedMarkdown; }
    public void setGeneratedMarkdown(String generatedMarkdown) { this.generatedMarkdown = generatedMarkdown; }

    public String getConfluencePageId() { return confluencePageId; }
    public void setConfluencePageId(String confluencePageId) { this.confluencePageId = confluencePageId; }

    
}
