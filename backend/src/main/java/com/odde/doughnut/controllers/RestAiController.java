package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.AiGeneratedImage;
import com.odde.doughnut.controllers.json.AiTrainingFile;
import com.odde.doughnut.controllers.json.ChatRequest;
import com.odde.doughnut.controllers.json.ChatResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiApi;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {
  private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public RestAiController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @PostMapping("/{note}/completion")
  public AiCompletion getCompletion(
      @PathVariable(name = "note") Note note, @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getAiCompletion(aiCompletionParams, note);
  }

  @PostMapping("/chat")
  public ChatResponse chat(
      @RequestParam(value = "note") Note note, @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    String userMessage = request.getUserMessage();
    String assistantMessage = this.aiAdvisorService.chatToAi(note, userMessage);
    return new ChatResponse(assistantMessage);
  }

  @PostMapping("/generate-image")
  public AiGeneratedImage generateImage(@RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(aiAdvisorService.getImage(aiCompletionParams.prompt));
  }

  @GetMapping("/training-files")
  public List<AiTrainingFile> retrieveTrainingFiles() {
    currentUser.assertLoggedIn();
    return aiAdvisorService.listTrainingFiles();
  }

  @PostMapping("'/trigger-finetune/{fileId}")
  public void triggerFineTune(@PathVariable(name = "fileId") String fileId) {
    currentUser.assertLoggedIn();
    aiAdvisorService.triggerFineTune(fileId);
  }
}
