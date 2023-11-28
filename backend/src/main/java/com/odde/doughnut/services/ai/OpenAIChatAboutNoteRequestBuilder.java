package com.odde.doughnut.services.ai;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.entities.Note;
import com.theokanning.openai.completion.chat.*;
import java.util.ArrayList;
import java.util.List;

public class OpenAIChatAboutNoteRequestBuilder {
  public static String askClarificationQuestion = "ask_clarification_question";
  String model = null;
  private List<ChatMessage> messages = new ArrayList<>();
  private List<ChatFunction> functions = new ArrayList<>();
  private int maxTokens;

  public OpenAIChatAboutNoteRequestBuilder() {}

  public OpenAIChatAboutNoteRequestBuilder systemBrief() {
    return addMessage(
        ChatMessageRole.SYSTEM,
        "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
  }

  public OpenAIChatAboutNoteRequestBuilder model(String modelName) {
    this.model = modelName;
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder contentOfNoteOfCurrentFocus(Note note) {
    String noteOfCurrentFocus = note.getNoteDescription();
    return addMessage(ChatMessageRole.SYSTEM, noteOfCurrentFocus);
  }

  public OpenAIChatAboutNoteRequestBuilder userInstructionToGenerateQuestionWithFunctionCall() {
    functions.add(
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(MCQWithAnswer.class, null)
            .build());

    String messageBody =
        """
  Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

  1. Generate a MCQ based on the note in the current context path
  2. Only the top-level of the context path is visible to the user.
  3. Provide 2 to 4 choices with only 1 correct answer.
  4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
  5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

  Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
  """;
    return addMessage(ChatMessageRole.USER, messageBody);
  }

  public OpenAIChatAboutNoteRequestBuilder instructionForDetailsCompletion(
      AiCompletionParams noteDetailsCompletion) {
    functions.add(
        ChatFunction.builder()
            .name("complete_note_details")
            .description("Text completion for the details of the note of focus")
            .executor(NoteDetailsCompletion.class, null)
            .build());
    functions.add(
        ChatFunction.builder()
            .name(askClarificationQuestion)
            .description("Ask question to get more context")
            .executor(ClarifyingQuestion.class, null)
            .build());
    addMessage(
        ChatMessageRole.USER,
        ("Please complete the concise details of the note of focus. Keep it short."
                + " Don't make assumptions about the context. Ask for clarification through function `%s` if my request is ambiguous."
                + " The current details in JSON format are: \n%s")
            .formatted(
                askClarificationQuestion,
                defaultObjectMapper().valueToTree(noteDetailsCompletion).toPrettyString()));
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder maxTokens(int maxTokens) {
    this.maxTokens = maxTokens;
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return messages;
  }

  public ChatCompletionRequest build() {
    if (model == null) {
      throw new RuntimeException("model is not set");
    }
    ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder =
        ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            //
            // an effort has been made to make the api call more responsive by using stream(true)
            // however, due to the library limitation, we cannot do it yet.
            // find more details here:
            //    https://github.com/TheoKanning/openai-java/issues/83
            .stream(false)
            .n(1);
    if (!functions.isEmpty()) {
      requestBuilder.functions(functions);
    }
    return requestBuilder.maxTokens(maxTokens).build();
  }

  public OpenAIChatAboutNoteRequestBuilder evaluateQuestion(MCQWithAnswer question) {
    functions.add(
        ChatFunction.builder()
            .name("evaluate_question")
            .description("answer and evaluate the feasibility of the question")
            .executor(QuestionEvaluation.class, null)
            .build());

    MultipleChoicesQuestion clone = new MultipleChoicesQuestion();
    clone.stem = question.stem;
    clone.choices = question.choices;

    String messageBody =
        """
Please assume the role of a learner, who has learned the note of focus as well as many other notes.
Only the top-level of the context path is visible to you.
Without the specific note of focus and its more detailed contexts revealed to you,
please critically check if the following question makes sense and is possible to you:

%s

"""
            .formatted(clone.toJsonString());
    return addMessage(ChatMessageRole.USER, messageBody);
  }

  public OpenAIChatAboutNoteRequestBuilder chatMessage(String userMessage) {
    ChatMessageRole role = ChatMessageRole.USER;
    return addMessage(role, userMessage);
  }

  public OpenAIChatAboutNoteRequestBuilder addMessage(ChatMessageRole role, String userMessage) {
    messages.add(new ChatMessage(role.value(), userMessage));
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder questionSchemaInPlainChat() {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    String schemaString;
    try {
      JsonSchema schema = jsonSchemaGenerator.generateSchema(MCQWithAnswer.class);
      schemaString = objectMapper.writeValueAsString(schema);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return addMessage(
        ChatMessageRole.SYSTEM,
        "When generating a question, please use this json structure:\n" + schemaString);
  }

  public OpenAIChatAboutNoteRequestBuilder
      userInstructionToGenerateQuestionWithGPT35FineTunedModel() {
    String messageBody =
        "Please assume the role of a Memory Assistant. Generate a MCQ based on the note of current focus in its context path.";

    return addMessage(ChatMessageRole.USER, messageBody);
  }

  public OpenAIChatAboutNoteRequestBuilder evaluationResult(QuestionEvaluation questionEvaluation) {
    ChatMessage msg = new ChatMessage(ChatMessageRole.ASSISTANT.value(), null);
    JsonNode arguments = new ObjectMapper().valueToTree(questionEvaluation);
    msg.setFunctionCall(new ChatFunctionCall("evaluate_question", arguments));
    messages.add(msg);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder generatedQuestion(MCQWithAnswer preservedQuestion) {
    ChatMessage msg = new ChatMessage(ChatMessageRole.ASSISTANT.value(), null);
    JsonNode arguments = new ObjectMapper().valueToTree(preservedQuestion);
    msg.setFunctionCall(
        new ChatFunctionCall("ask_single_answer_multiple_choice_question", arguments));
    messages.add(msg);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder answerClarifyingQuestion(String answerFromUser) {
    ChatMessage callResponse = new ChatMessage(ChatMessageRole.FUNCTION.value(), answerFromUser);
    callResponse.setName("ask_clarification_question");
    messages.add(callResponse);
    return this;
  }
}
