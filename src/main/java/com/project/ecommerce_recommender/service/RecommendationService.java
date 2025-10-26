package com.project.ecommerce_recommender.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecommerce_recommender.dto.ProductDTO;
import com.project.ecommerce_recommender.model.Product;
import com.project.ecommerce_recommender.model.Transaction;
import com.project.ecommerce_recommender.repository.ProductRepository;
import com.project.ecommerce_recommender.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ProductDTO> recommendProducts(Long userId, int topN) {
        try {
            // Get latest 5 transactions
            List<Transaction> userTx = transactionRepository
                    .findTop5ByUser_IdOrderByCreatedAtDesc(userId);

            if (userTx.isEmpty()) {
                return productRepository.findAll()
                        .stream()
                        .map(this::toDTO)
                        .collect(Collectors.toList());
            }

            // Last interacted product (force fetch lazy)
            Product lastProduct = userTx.get(userTx.size() - 1).getProduct();
            List<Double> lastFeatures = objectMapper.readValue(
                    lastProduct.getFeatures(),
                    new TypeReference<List<Double>>() {}
            );

            long totalTx = transactionRepository.count();

            Map<Product, Double> scores = new HashMap<>();
            for (Product p : productRepository.findAll()) {
                if (p.getId().equals(lastProduct.getId())) continue;

                List<Double> f = objectMapper.readValue(p.getFeatures(), new TypeReference<List<Double>>() {});
                double sim = cosineSimilarity(lastFeatures, f);
                double popularity = totalTx == 0 ? 0 :
                        transactionRepository.countByProductId(p.getId()) / (double) totalTx;

                scores.put(p, 0.7 * sim + 0.3 * popularity);
            }

            return scores.entrySet().stream()
                    .sorted(Map.Entry.<Product, Double>comparingByValue().reversed())
                    .limit(topN)
                    .map(entry -> toDTO(entry.getKey()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to recommend products for userId={}", userId, e);
        }

        return Collections.emptyList();
    }

    private ProductDTO toDTO(Product product) {
        try {
            List<Double> features = objectMapper.readValue(
                    product.getFeatures(),
                    new TypeReference<List<Double>>() {}
            );
            return new ProductDTO(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    product.getPrice(),
                    features
            );
        } catch (Exception e) {
            log.error("Failed to parse features for productId={}", product.getId(), e);
            return new ProductDTO(product.getId(), product.getName(), product.getCategory(), product.getPrice(), Collections.emptyList());
        }
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
