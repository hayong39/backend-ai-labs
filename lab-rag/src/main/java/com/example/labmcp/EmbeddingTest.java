package com.example.labmcp;

import java.util.Arrays;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class EmbeddingTest implements CommandLineRunner {

  private final EmbeddingModel embeddingModel;

  public EmbeddingTest(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  @Override
  public void run(String... args) {
    float[] vector = embeddingModel.embed("passage: 암 진단 시 보험금을 지급한다.");

    System.out.println("벡터 차원: " + vector.length);
    System.out.println(Arrays.toString(Arrays.copyOf(vector, 5)));
  }
}
