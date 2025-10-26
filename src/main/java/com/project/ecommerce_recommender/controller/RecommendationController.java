package com.project.ecommerce_recommender.controller;

import com.project.ecommerce_recommender.model.Product;
import com.project.ecommerce_recommender.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public List<Product> recommend(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "5") int topN) {
        try {
            return recommendationService.recommendProducts(userId, topN);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
