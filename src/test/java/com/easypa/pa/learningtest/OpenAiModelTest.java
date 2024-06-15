package com.easypa.pa.learningtest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.MimeTypeUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class OpenAiModelTest {
    @Autowired
    private OpenAiChatModel chatModel;

    @DisplayName("")
    @Test
    public void chatModelByAutoConfigured() {
        assertThat(chatModel).isNotNull();
        OpenAiChatOptions defaultOptions = (OpenAiChatOptions) chatModel.getDefaultOptions();
        assertThat(defaultOptions.getModel()).isEqualTo("gpt-3.5-turbo");
    }

    @DisplayName("")
    @Test
    public void overrideDefaultOptionsAtRunTime() {
        // given
        ChatResponse chatResponse = chatModel.call(
                new Prompt(
                        "Generate the names of 5 famous pirates",
                        OpenAiChatOptions.builder()
                                .withModel(OpenAiApi.ChatModel.GPT_4_TURBO.getValue())
                                .withTemperature(0.4F)
                                .build()
                ));

        assertThat(chatResponse).isNotNull();
        log.info("### chatResponse: {}", chatResponse);
    }

    @DisplayName("")
    @Test
    public void userMessageWithImage() throws URISyntaxException, MalformedURLException {
        // given
        URL testImgUrl = new URI("https://docs.spring.io/spring-ai/reference/1.0-SNAPSHOT/_images/multimodal.test.png").toURL();
        Media testMedia = new Media(MimeTypeUtils.IMAGE_PNG, testImgUrl);

        UserMessage userMessage = new UserMessage("Explain what do you see on this picture?", List.of(testMedia));

        // when
        ChatResponse chatResponse = chatModel.call(
                new Prompt(
                        List.of(userMessage),
                        OpenAiChatOptions.builder()
                                .withModel(OpenAiApi.ChatModel.GPT_4_O.getValue())
                                .build()
                ));

        // then
        log.info("### chatResponse: {}", chatResponse);
    }
}
