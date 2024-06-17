package com.easypa.pa.learningtest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.MilvusVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
public class MilvusVectorStoreTest {

    @Autowired
    private MilvusVectorStore vectorStore;

    @DisplayName("")
    @Test
    public void milvusVectorStore_NotNull() {
        assertNotNull(this.vectorStore);
    }

    @DisplayName("")
    @Test
    public void test() {
        // given
        List<Document> documents =
                List.of(new Document("Andrew is a software engineer and is currently learning about Spring AI."));

        // when
        vectorStore.add(documents);

        // then
        List<Document> resultDocuments = vectorStore.similaritySearch(SearchRequest.query("who is andrew?").withTopK(5));

        assertThat(resultDocuments).isNotEmpty();
        log.info("### resultDocuments: {}", resultDocuments);
    }
}
