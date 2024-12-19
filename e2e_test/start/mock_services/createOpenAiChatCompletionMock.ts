import ServiceMocker from '../../support/ServiceMocker'
import { MessageToMatch } from './MessageToMatch'

type ToolCall = {
  id: string
  type: 'function'
  function: {
    name: string
    arguments: string
  }
}

type ToolCalls = {
  role: 'assistant'
  tool_calls: ToolCall[]
}

type TextBasedMessage = {
  role: 'user' | 'assistant' | 'system'
  content: string
}

type BodyToMatch = {
  messages?: MessageToMatch[]
  model?: string
}

type ChatMessageInResponse = TextBasedMessage | ToolCalls

const openAiChatCompletionStubber = (
  serviceMocker: ServiceMocker,
  bodyToMatch: BodyToMatch,
  bodyNotToMatch?: BodyToMatch
) => {
  const stubChatCompletion = (
    message: ChatMessageInResponse,
    finishReason: 'length' | 'stop' | 'function_call'
  ): Promise<void> => {
    return serviceMocker.mockPostMatchsAndNotMatches(
      `/chat/completions`,
      bodyToMatch,
      bodyNotToMatch,
      [
        {
          object: 'chat.completion',
          choices: [
            {
              message,
              index: 0,
              finish_reason: finishReason,
            },
          ],
        },
      ]
    )
  }

  const stubJsonSchemaResponse = (argumentsString: string) => {
    return stubChatCompletion(
      {
        role: 'assistant',
        content: argumentsString,
      },
      'stop'
    )
  }

  return {
    stubQuestionGeneration(argumentsString: string) {
      return stubJsonSchemaResponse(argumentsString)
    },
    requestDoesNotMessageMatch(message: MessageToMatch) {
      return openAiChatCompletionStubber(serviceMocker, bodyToMatch, {
        messages: [message],
      })
    },
    stubAudioTranscriptToText(argumentsString: string) {
      return stubJsonSchemaResponse(
        JSON.stringify({
          completion: argumentsString,
          deleteFromEnd: 0,
        })
      )
    },
    stubQuestionEvaluation(argumentsString: string) {
      return stubJsonSchemaResponse(argumentsString)
    },
  }
}

const createOpenAiChatCompletionMock = (serviceMocker: ServiceMocker) => {
  return {
    requestMessageMatches(message: MessageToMatch) {
      return this.requestMessagesMatch([message])
    },
    requestMessagesMatch(messages: MessageToMatch[]) {
      return this.requestMatches({ messages })
    },
    requestMatches(bodyToMatch: BodyToMatch) {
      return openAiChatCompletionStubber(serviceMocker, bodyToMatch)
    },
  }
}

export default createOpenAiChatCompletionMock
