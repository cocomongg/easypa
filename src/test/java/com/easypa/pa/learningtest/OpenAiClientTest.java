package com.easypa.pa.learningtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.vectorstore.MilvusVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@SpringBootTest
public class OpenAiClientTest {

    @Autowired
    private ChatClient.Builder clientBuilder;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ObjectMapper objectMapper;

    private ChatClient chatClient;

    record ActorFilms(String actor, List<String> movies) {
    }

    @BeforeEach
    public void setup() {
        chatClient = clientBuilder.build();
    }

    @DisplayName("")
    @Test
    public void createChatClient() {
        assertThat(clientBuilder).isNotNull();
        assertThat(chatModel).isNotNull();

        ChatClient chatClientByAutoConfigured = clientBuilder.build();

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        ChatClient chatClientByCustom = builder.build();

        assertThat(chatClientByAutoConfigured).isNotNull();
        assertThat(chatClientByCustom).isNotNull();
    }

    @DisplayName("")
    @Test
    public void generation() {
        // given, when
        String content = this.chatClient.prompt()
                .user("hello?")
                .call()
                .content();

        // then
        assertThat(content).isNotEmpty();
        log.info("### content: {}", content);
    }

    @DisplayName("")
    @Test
    public void chatResponse() throws JsonProcessingException {
        // given, when
        ChatResponse chatResponse = this.chatClient.prompt()
                .user("hello?")
                .call()
                .chatResponse();

        ChatResponseMetadata responseMetadata = chatResponse.getMetadata();
        List<Generation> generations = chatResponse.getResults();
        Generation generation = chatResponse.getResult();

        // then
        assertThat(responseMetadata).isNotNull();
        assertThat(generations)
                .isNotEmpty()
                .hasSize(1);
        assertThat(generation).isNotNull();
        assertEquals(generations.get(0), generation);

        log.info("### chatResponse: {}", chatResponse);
        log.info("### chatResponse Json: {}", objectMapper.writeValueAsString(chatResponse));
        log.info("### responseMetadata: {}", responseMetadata);
        log.info("### generations: {}", generations);
        log.info("### generation: {}", generation);

        // then - responseMetadata
        RateLimit rateLimit = responseMetadata.getRateLimit(); // info of ratelimit
        PromptMetadata promptMetadata = responseMetadata.getPromptMetadata(); //
        Usage usage = responseMetadata.getUsage(); // info of generation token count
        log.info("------- responseMetadata -------");
        log.info("### rateLimit: {}", rateLimit);
        log.info("### promptMetadata: {}", promptMetadata);
        log.info("### usage: {}", usage);

        // then - generation
        log.info("------- Generation -------");
        ChatGenerationMetadata generationMetadata = generation.getMetadata(); // finish reason and content filter meta
        AssistantMessage output = generation.getOutput();

        log.info("### generationMeta: {}", generationMetadata);
        log.info("### assistantMessage: {}", output);
    }

    @DisplayName("")
    @Test
    public void chatStreamResponse() throws InterruptedException {
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                .user("hello?")
                .stream()
                .chatResponse();

        chatResponseFlux.map(chatResponse -> {
            Generation generation = chatResponse.getResult();
            AssistantMessage output = generation.getOutput();
            ChatGenerationMetadata metadata = generation.getMetadata();

            String content = output.getContent();
            String finishReason = metadata.getFinishReason();

            if(StringUtils.hasText(finishReason)) {
                return String.format("finish reason: %s", finishReason);
            }

            if(!StringUtils.hasText(content)) {
                return "";
            }

            return content;
        }).subscribe(
                log::info,
                error-> log.info("", error),
                () -> log.info("end")
        );

        Thread.sleep(10 * 1000L);
    }

    @DisplayName("")
    @Test
    public void chatResponseByEntity() {
        // given, when
        List<ActorFilms> actorFilms = chatClient.prompt()
                .user("Generate the filmography of 5 movies for Tom Hanks and Bill Murray.")
                .call()
                .entity(new ParameterizedTypeReference<List<ActorFilms>>() {
                });

        // then
        assertThat(actorFilms)
                .isNotEmpty()
                .hasSize(2);

        assertThat(actorFilms.get(0).movies())
                .isNotEmpty()
                .hasSize(5);
        log.info("### actorFilms: {}", actorFilms);
    }

    @DisplayName("")
    @Test
    public void chatClientWithDefaultSystem() {
        // given
        ChatClient chatClient = clientBuilder
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

    @DisplayName("")
    @Test
    public void chatClientWithChatMemoryAdvisor() {
        // given
        String conversationId = "test-conversation-id";
        ChatMemory chatMemory = new InMemoryChatMemory();
        Message userMessage = new UserMessage("My name is andrew, and my job is software engineer");
        AssistantMessage assistantMessage = new AssistantMessage("Ok!");

        List<Message> storedMessages = List.of(userMessage, assistantMessage);
        chatMemory.add(conversationId, storedMessages);

        // when
        String content = chatClient.prompt()
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .user("Do you know my name and job?")
                .call()
                .content();

        // then
        assertThat(content).isNotEmpty();
        log.info("### content: {}", content);
    }
}
