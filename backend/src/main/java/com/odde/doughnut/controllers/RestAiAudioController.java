package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AiAdvisorWithStorageService;
import com.odde.doughnut.services.AiServiceFactory;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/audio")
class RestAiAudioController {

  OtherAiServices otherAiServices;
  private final ModelFactoryService modelFactoryService;
  private final AiAdvisorWithStorageService aiAdvisorWithStorageService;

  public RestAiAudioController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService) {
    AiServiceFactory aiServiceFactory = new AiServiceFactory(openAiApi);
    this.otherAiServices = aiServiceFactory.getOtherAiServices();
    this.modelFactoryService = modelFactoryService;
    this.aiAdvisorWithStorageService =
        new AiAdvisorWithStorageService(aiServiceFactory, modelFactoryService);
  }

  @PostMapping(
      path = "/audio-to-text",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public Optional<TextFromAudio> audioToText(@Valid @ModelAttribute AudioUploadDTO audioFile)
      throws IOException {
    String filename = audioFile.getUploadAudioFile().getOriginalFilename();
    byte[] bytes = audioFile.getUploadAudioFile().getBytes();
    return otherAiServices.getTextFromAudio(
        audioFile.getPreviousNoteDetails(),
        filename,
        bytes,
        getGlobalSettingsService().globalSettingOthers().getValue());
  }

  @PostMapping(
      path = "/audio-to-text/{noteId}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public TextFromAudio audioToTextForNote(
      @PathVariable("noteId") @Schema(type = "integer") Note note,
      @Valid @ModelAttribute AudioUploadDTO audioFile)
      throws IOException {
    String filename = audioFile.getUploadAudioFile().getOriginalFilename();
    byte[] bytes = audioFile.getUploadAudioFile().getBytes();
    String transcriptionFromAudio = otherAiServices.getTranscriptionFromAudio(filename, bytes);
    return new TextFromAudio();
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
