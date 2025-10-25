CREATE TABLE item_embeddings (
    product_id BIGINT PRIMARY KEY,
    embedding JSON,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
