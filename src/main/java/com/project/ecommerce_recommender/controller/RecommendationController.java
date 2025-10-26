package com.project.ecommerce_recommender.controller;


import com.project.ecommerce_recommender.dto.ProductDTO;
import com.project.ecommerce_recommender.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommend/{userId}")
    public List<ProductDTO> recommend(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "5") int topN) {
        return recommendationService.recommendProducts(userId, topN);
    }
}
