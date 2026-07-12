-- V14__add_store_payments_table.sql
CREATE TABLE store_payments (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KES',
    status VARCHAR(50) NOT NULL,
    tracking_id VARCHAR(100),
    merchant_reference VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
