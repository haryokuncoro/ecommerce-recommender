package com.project.ecommerce_recommender.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecommerce_recommender.model.Product;
import com.project.ecommerce_recommender.model.Transaction;
import com.project.ecommerce_recommender.repository.ProductRepository;
import com.project.ecommerce_recommender.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service @Log4j2
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public List<Product> recommendProducts(Long userId, int topN) throws Exception {
        try {
            List<Transaction> userTx = transactionRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);

            if(userTx.isEmpty()) return productRepository.findAll();

            Product lastProduct = productRepository.findById(userTx.get(userTx.size()-1).getProduct().getId())
                    .orElseThrow();

            List<Double> lastFeatures = objectMapper.readValue(lastProduct.getFeatures(), new TypeReference<>() {});

            Map<Product, Double> scores = new HashMap<>();
            for(Product p : productRepository.findAll()) {
                if(p.getId().equals(lastProduct.getId())) continue;

                List<Double> f = objectMapper.readValue(p.getFeatures(), new TypeReference<>() {});
                double sim = cosineSimilarity(lastFeatures, f);
                double popularity = transactionRepository.countByProductId(p.getId()) / (double) transactionRepository.count();
                scores.put(p, 0.7*sim + 0.3*popularity);
            }

            return scores.entrySet().stream()
                    .sorted(Map.Entry.<Product, Double>comparingByValue().reversed())
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .toList();
        }catch (Exception e){
            log.error("fail to recommendProducts, userId={}", userId, e);
        }
        return new ArrayList<>();
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot=0, normA=0, normB=0;
        for(int i=0;i<a.size();i++){
            dot += a.get(i)*b.get(i);
            normA += a.get(i)*a.get(i);
            normB += b.get(i)*b.get(i);
        }
        return dot / (Math.sqrt(normA)*Math.sqrt(normB));
    }
}

