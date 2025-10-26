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

    public String generateDummyProducts() throws Exception {

        List<Map<String, Object>> rawProducts = new ArrayList<>();

        // Gunakan empty string untuk null
        rawProducts.add(Map.of("name","Headphone X","category","Elektronik","brand","Sony","price",15000,"sold",500,"color","Hitam","aroma",""));
        rawProducts.add(Map.of("name","Headphone Y","category","Elektronik","brand","Xiaomi","price",12000,"sold",1000,"color","Putih","aroma",""));
        rawProducts.add(Map.of("name","Sneakers A","category","Fashion","brand","Nike","price",8000,"sold",200,"color","Merah","aroma",""));
        rawProducts.add(Map.of("name","Sneakers B","category","Fashion","brand","Adidas","price",9000,"sold",300,"color","Hitam","aroma",""));
        rawProducts.add(Map.of("name","Perfume A","category","Beauty","brand","Dior","price",200000,"sold",50,"color","","aroma","Floral"));
        rawProducts.add(Map.of("name","Perfume B","category","Beauty","brand","Chanel","price",180000,"sold",70,"color","","aroma","Citrus"));
        rawProducts.add(Map.of("name","Laptop A","category","Elektronik","brand","Dell","price",100000,"sold",150,"color","","aroma",""));
        rawProducts.add(Map.of("name","Laptop B","category","Elektronik","brand","Asus","price",90000,"sold",200,"color","","aroma",""));
        rawProducts.add(Map.of("name","T-Shirt A","category","Fashion","brand","Uniqlo","price",150000,"sold",400,"color","Biru","aroma",""));
        rawProducts.add(Map.of("name","T-Shirt B","category","Fashion","brand","H&M","price",120000,"sold",300,"color","Hitam","aroma",""));

        // Category & Brand map
        Map<String,Integer> categoryMap = Map.of("Elektronik",1,"Fashion",2,"Beauty",3);
        Map<String,Double> brandScore = Map.of(
                "Sony",0.8,"Xiaomi",0.7,"Nike",0.9,"Adidas",0.85,
                "Dior",0.95,"Chanel",0.9,"Dell",0.85,"Asus",0.8,
                "Uniqlo",0.75,"H&M",0.7
        );

        double maxPrice = rawProducts.stream().mapToDouble(p -> (int)p.get("price")).max().orElse(1);
        double maxSold  = rawProducts.stream().mapToDouble(p -> (int)p.get("sold")).max().orElse(1);

        // One-hot map
        Map<String,List<Double>> colorMap = Map.of(
                "Hitam", List.of(1.0,0.0,0.0,0.0),
                "Putih", List.of(0.0,1.0,0.0,0.0),
                "Merah", List.of(0.0,0.0,1.0,0.0),
                "Biru",  List.of(0.0,0.0,0.0,1.0),
                "",      List.of(0.0,0.0,0.0,0.0)
        );
        Map<String,List<Double>> aromaMap = Map.of(
                "Floral", List.of(1.0,0.0),
                "Citrus", List.of(0.0,1.0),
                "",      List.of(0.0,0.0)
        );

        // Generate Product entities
        List<Product> products = new ArrayList<>();
        for (Map<String,Object> p : rawProducts) {
            Product product = new Product();
            product.setName((String)p.get("name"));
            product.setCategory((String)p.get("category"));
            product.setBrand((String)p.get("brand"));
            product.setPrice((Integer)p.get("price"));

            double priceNorm = (Integer)p.get("price") / maxPrice;
            double popularity = (Integer)p.get("sold") / maxSold;
            int categoryEncode = categoryMap.get(p.get("category"));
            double brandScoreVal = brandScore.get(p.get("brand"));

            List<Double> features = new ArrayList<>();
            features.add(priceNorm);           // numeric
            features.add(brandScoreVal);       // numeric
            features.add(popularity);          // numeric
            features.add((double)categoryEncode); // numeric
            features.addAll(colorMap.get(p.get("color")));  // categorical
            features.addAll(aromaMap.get(p.get("aroma")));  // categorical

            product.setFeatures(objectMapper.writeValueAsString(features));
            products.add(product);
        }

        productRepository.saveAll(products);
        return "Dummy products generated with fixed-length feature vectors!";
    }
}
