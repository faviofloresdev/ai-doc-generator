package com.agent.doc.generator.service;

public interface ConfluenceService {

    public String getExistingContent(String pageId);

    public void publishPage(String pageId, String title, String markdownContent);
}