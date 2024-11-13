package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAssistantFacade;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.assistant.Assistant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  private final OtherAiServices otherAiServices;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final ModelFactoryService modelFactoryService;
  private final AiAssistantFacade aiAssistantFacade;

  public RestAiController(
      ModelFactoryService modelFactoryService,
      AiAssistantFacade aiAssistantFacade,
      OtherAiServices otherAiServices,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.aiAssistantFacade = aiAssistantFacade;
    this.otherAiServices = otherAiServices;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/dummy")
  public DummyForGeneratingTypes dummyEntryToGenerateDataTypesThatAreRequiredInEventStream()
      throws HttpMediaTypeNotAcceptableException {
    throw new HttpMediaTypeNotAcceptableException("dummy");
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(otherAiServices.getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return otherAiServices.getAvailableGptModels();
  }

  @PostMapping("/recreate-all-assistants")
  @Transactional
  public Map<String, String> recreateAllAssistants() throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    Assistant assistant = aiAssistantFacade.recreateDefaultAssistant(currentUTCTimestamp);
    Map<String, String> result = new HashMap<>();
    result.put(assistant.getName(), assistant.getId());
    return result;
  }

  @PostMapping("/recreate-notebook-assistant/{notebook}")
  @Transactional
  public NotebookAssistant recreateNotebookAssistant(
      @PathVariable(value = "notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody NotebookAssistantCreationParams notebookAssistantCreationParams)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    NotebookAssistant notebookAssistant =
        aiAssistantFacade.recreateNotebookAssistant(
            currentUTCTimestamp,
            currentUser.getEntity(),
            notebook,
            notebookAssistantCreationParams.getAdditionalInstruction());
    this.modelFactoryService.save(notebookAssistant);
    return notebookAssistant;
  }

  @PostMapping("/submit-tool-result/{threadId}/{runId}/{toolCallId}")
  @Transactional
  public void submitToolCallResult(
      @PathVariable String threadId,
      @PathVariable String runId,
      @PathVariable String toolCallId,
      @RequestBody ToolCallResult result)
      throws JsonProcessingException {
    currentUser.assertLoggedIn();
    aiAssistantFacade.getAssistantRunService(threadId, runId).submitToolOutputs(toolCallId, result);
  }

  @PostMapping("/cancel-run/{threadId}/{runId}")
  @Transactional
  public void cancelRun(@PathVariable String threadId, @PathVariable String runId) {
    currentUser.assertLoggedIn();
    aiAssistantFacade.getAssistantRunService(threadId, runId).cancelRun();
  }

  @PostMapping("/suggest-topic-title/{note}")
  @Transactional
  public SuggestedTopicDTO suggestTopicTitle(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    String title = aiAssistantFacade.suggestTopicTitle(note);
    return new SuggestedTopicDTO(title);
  }
}
