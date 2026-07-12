-- V12__marketplace_architecture.sql

-- 1. Create Platform Configs
CREATE TABLE platform_configs (
    id UUID PRIMARY KEY,
    config_key VARCHAR(255) UNIQUE NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    parent_id UUID,
    type VARCHAR(50) NOT NULL, -- PRODUCT / SERVICE
    image_url VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE / INACTIVE
    commission_rate DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 3. Modify Products to link to Categories
ALTER TABLE products ADD COLUMN category_id UUID;
ALTER TABLE products ADD COLUMN subcategory_id UUID;

ALTER TABLE products ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE products ADD CONSTRAINT fk_product_subcategory FOREIGN KEY (subcategory_id) REFERENCES categories(id) ON DELETE SET NULL;

-- 4. Modify Stores (Location, Verification, Business info)
ALTER TABLE stores ADD COLUMN city VARCHAR(100);
ALTER TABLE stores ADD COLUMN country VARCHAR(100);
ALTER TABLE stores ADD COLUMN business_hours JSONB;
ALTER TABLE stores ADD COLUMN logo_url VARCHAR(255);
ALTER TABLE stores ADD COLUMN banner_url VARCHAR(255);
ALTER TABLE stores ADD COLUMN verification_status VARCHAR(50) DEFAULT 'PENDING'; -- PENDING / APPROVED / REJECTED

-- 5. Create Services
CREATE TABLE services (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    vendor_id UUID NOT NULL,
    store_id UUID NOT NULL,
    category_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    business_hours JSONB,
    portfolio_urls JSONB,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_vendor FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_service_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- 6. Create Wallets
CREATE TABLE vendor_wallets (
    id UUID PRIMARY KEY,
    vendor_id UUID UNIQUE NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    pending_balance DECIMAL(15,2) DEFAULT 0.00,
    withdrawable_balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_vendor FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. Create Transactions
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL, -- SALE, COMMISSION, WITHDRAWAL, REFUND
    amount DECIMAL(15,2) NOT NULL,
    reference_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'COMPLETED', -- PENDING, COMPLETED, FAILED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES vendor_wallets(id) ON DELETE CASCADE
);

-- 8. Create Withdrawals
CREATE TABLE withdrawals (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- MPESA, PAYPAL, STRIPE
    account_details VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_withdrawal_wallet FOREIGN KEY (wallet_id) REFERENCES vendor_wallets(id) ON DELETE CASCADE
);

-- 9. Create Vendor Verifications
CREATE TABLE vendor_verifications (
    id UUID PRIMARY KEY,
    store_id UUID UNIQUE NOT NULL,
    document_urls JSONB,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    rejection_reason TEXT,
    otp_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);
