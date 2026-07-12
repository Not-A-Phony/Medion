CREATE TABLE product_images (
    product_id UUID NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_product_image FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);
