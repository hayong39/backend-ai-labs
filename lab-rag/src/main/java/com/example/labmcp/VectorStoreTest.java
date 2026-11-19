package com.example.labmcp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(1)
public class VectorStoreTest implements CommandLineRunner {

  private final VectorStore vectorStore;
  private final EmbeddingModel embeddingModel;

  public VectorStoreTest(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
    this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
  }

  @Override
  public void run(String... args) throws IOException {
    List<Policy> policies;
    try (InputStream input = new ClassPathResource("policies.json").getInputStream()) {
      policies = JsonMapper.shared().readValue(input, new TypeReference<List<Policy>>() {});
    }

    // embedding and storing logic
    List<Document> documents =
        policies.stream()
            .map(
                policy ->
                    new Document(policy.id(), policy.content(), Map.of("title", policy.title())))
            .toList();
    vectorStore.add(documents);

    // debugging logic
    System.out.printf("벡터스토어 저장 완료 (%d건)%n", documents.size());
    for (Document document : documents) {
      float[] vector = embeddingModel.embed(document);
      System.out.printf(
          "%s | %s | 벡터 차원: %d | 앞 5개: %s%n",
          document.getId(),
          document.getMetadata().get("title"),
          vector.length,
          Arrays.toString(Arrays.copyOf(vector, Math.min(vector.length, 5))));
    }

    // search logic
    String query = "암 진단 시 보험금 지급 여부";
    List<Document> results = vectorStore.similaritySearch(query);
    System.out.printf("검색어: %s | 검색 결과: %d건%n", query, results.size());
    for (Document result : results) {
      float[] vector = embeddingModel.embed(result);
      System.out.printf(
          "%s | %s | 벡터 차원: %d | 앞 5개: %s%n",
          result.getId(),
          result.getMetadata().get("title"),
          vector.length,
          Arrays.toString(Arrays.copyOf(vector, Math.min(vector.length, 5))));
    }

    // store vector store to disk
    ((SimpleVectorStore) vectorStore).save(new File("policies-vectors.json"));
  }

  private record Policy(String id, String title, String content) {}
}
