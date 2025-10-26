package com.project.ecommerce_recommender.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.ecommerce_recommender.model.Transaction;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByProductId(Long productId);
}
