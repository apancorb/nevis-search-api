package com.nevis.search.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${spring.ai.openai.api-key:}'.length() > 0")
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public OpenAiChatModel openAiChatModel(
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.api-key}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model) {
        log.info("OpenAI API key detected - enabling document summarization with model: {}", model);
        OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
