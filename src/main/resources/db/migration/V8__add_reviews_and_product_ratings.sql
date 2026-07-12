-- Add rating and review count to products
ALTER TABLE products ADD COLUMN average_rating DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE products ADD COLUMN review_count INTEGER DEFAULT 0;

-- Create reviews table
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    user_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT unique_product_user_review UNIQUE (product_id, user_id)
);
