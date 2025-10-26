# Product Recommendation Service

A simple **Java Spring Boot** project that recommends products using a mix of **content similarity** and **popularity**.

---

## 🚀 Features
- Content-based recommendation using **cosine similarity**
- Popularity boost based on total transactions
- REST API to get product recommendations
- Uses Flyway for database migration
- Easy to integrate with any e-commerce app

---

## ⚙️ How It Works
Each product has a **feature vector** (e.g., `[0.75, 0.8, 0.3, 2, 1, 0, ...]`).

```java
double similarity = cosineSimilarity(productA, productB);
double popularity = transactionCount(product) / totalTransactions;
double finalScore = 0.7 * similarity + 0.3 * popularity;
````

The system recommends products with the **highest final scores**.

---

## 🧩 Example API

**Endpoint:**

```
GET /api/recommendations/{userId}
```

**Response:**

```json
[
  { "id": 1, "name": "Headphone X", "category": "Electronics", "score": 0.91 },
  { "id": 2, "name": "Sneakers B", "category": "Fashion", "score": 0.87 }
]
```
