package com.easypa.pa.learningtest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class OpenAiClientUsingDefaultTest {

    @Autowired
    private ChatClient.Builder builder;

    @DisplayName("")
    @Test
    public void chatClientWithDefaultSystem() {
        // given
        ChatClient chatClient = builder
                .defaultSystem("You are a friendly chat bot that answers question in the voice of a {voice}")
                .build();

        // when
        String content = chatClient.prompt()
                .system(sp -> sp.param("voice", "Wise Old Sage"))
                .user("I'm so tired")
                .call()
                .content();

        // then
        assertThat(content)
                .isNotEmpty();
        log.info("### content: {}", content);
    }
}
