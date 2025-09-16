package com.agent.doc.generator.controller;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.agent.doc.generator.service.ConfluenceService;
import com.agent.doc.generator.service.GeminiService;
import com.agent.doc.generator.service.GitService;
import com.agent.doc.generator.service.JiraService;


@Controller
@RequestMapping("/docs")
public class DocumentationController {

    private final GeminiService geminiService;
    private final JiraService jiraService;
    private final GitService gitService;
    private final ConfluenceService confluenceService;

    public DocumentationController(GeminiService geminiService, JiraService jiraService,
                                   GitService gitService, ConfluenceService confluenceService) {
        this.geminiService = geminiService;
        this.jiraService = jiraService;
        this.gitService = gitService;
        this.confluenceService = confluenceService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/docs/form";
    }
    
    @GetMapping("/form")
    public String form() {
        return "docForm"; // Thymeleaf template
    }

    @PostMapping("/publish")
    public String publish(@RequestParam String pageId,
                          @RequestParam String title,
                          @RequestParam String markdown) {

        confluenceService.publishPage(pageId, title, markdown);
        return "redirect:/docs/form?success";
    }
    
    @PostMapping("/generate")
    public String generate(
    		@RequestParam String jiraId,
            @RequestParam String repoUrl,
            @RequestParam String branch,
            @RequestParam String pageId,
            @RequestParam String proceso,
            @RequestParam String complejidad,
            @RequestParam String menu,
            @RequestParam String autor,
            @RequestParam String huRelacionados,
            Model model) {

    	String jiraContentRaw = jiraService.getIssueContent(jiraId);
    	String jiraText = extractPlainTextFromJira(jiraContentRaw); // función que convierte JSON de Jira a texto

        String gitChanges = gitService.getChangesForBranch(branch, huRelacionados);
        String confluenceContent = confluenceService.getExistingContent(pageId);
        
        String markdown = geminiService.generateMarkdown(
                jiraText, gitChanges, confluenceContent, proceso, complejidad, menu, autor, huRelacionados
        );

        model.addAttribute("markdown", markdown);
        model.addAttribute("pageId", pageId);
        
        return "docPreview";
    }

    private String extractPlainTextFromJira(String jiraContent) {
        try {
            JSONObject json = new JSONObject(jiraContent);
            JSONArray content = json.optJSONArray("content");
            if (content == null) return jiraContent;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.getJSONObject(i);
                JSONArray innerContent = block.optJSONArray("content");
                if (innerContent != null) {
                    for (int j = 0; j < innerContent.length(); j++) {
                        JSONObject textNode = innerContent.getJSONObject(j);
                        if ("text".equals(textNode.optString("type"))) {
                            sb.append(textNode.optString("text")).append(" ");
                        }
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            // Si no es JSON, devolver tal cual
            return jiraContent;
        }
    }


    
    @GetMapping("/testGemini")
    public String testGemini(
            @RequestParam(defaultValue = "Historia de ejemplo") String jiraStory,
            @RequestParam(defaultValue = "Cambios de Git de ejemplo") String gitChanges,
            @RequestParam(defaultValue = "Contenido existente de ejemplo") String existingContent
    ) {
        String markdown = geminiService.generateMarkdown(jiraStory, gitChanges, existingContent,"","","","","");
        return markdown;
    }
    
}