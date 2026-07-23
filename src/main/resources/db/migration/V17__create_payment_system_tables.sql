-- V17: Payment System Tables for Subscriptions, Store Wallets, and Payment Transactions
-- Creates payment_transactions, subscriptions, subscription_payments, store_wallets,
-- store_wallet_transactions, and payment_retry_logs tables

-- Payment Transactions Table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    store_id UUID REFERENCES stores(id),
    amount DECIMAL(10, 2) NOT NULL,
    mpesa_receipt_number VARCHAR(255),
    mpesa_transaction_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    external_reference_id VARCHAR(255) UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KES',
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    initiated_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id);
CREATE INDEX idx_payment_transactions_store_id ON payment_transactions(store_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_mpesa_receipt ON payment_transactions(mpesa_receipt_number);
CREATE INDEX idx_payment_transactions_external_ref ON payment_transactions(external_reference_id);
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at);

-- Subscriptions Table
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(id),
    subscription_plan_type VARCHAR(50) NOT NULL,
    monthly_flat_rate DECIMAL(10, 2),
    commission_percentage DECIMAL(5, 2),
    status VARCHAR(50) NOT NULL,
    current_billing_cycle_start TIMESTAMP,
    current_billing_cycle_end TIMESTAMP,
    auto_renewal BOOLEAN DEFAULT TRUE,
    last_payment_date TIMESTAMP,
    next_payment_date TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_store_id ON subscriptions(store_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_next_payment_date ON subscriptions(next_payment_date);

-- Subscription Payments Table (billing records)
CREATE TABLE subscription_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    billing_cycle_start TIMESTAMP NOT NULL,
    billing_cycle_end TIMESTAMP NOT NULL,
    amount_charged DECIMAL(10, 2) NOT NULL,
    due_date TIMESTAMP NOT NULL,
    paid_date TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    payment_transaction_id UUID REFERENCES payment_transactions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscription_payments_subscription_id ON subscription_payments(subscription_id);
CREATE INDEX idx_subscription_payments_status ON subscription_payments(status);
CREATE INDEX idx_subscription_payments_due_date ON subscription_payments(due_date);

-- Store Wallets Table (separate from vendor_wallets - keyed by store)
CREATE TABLE store_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(id),
    available_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    locked_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_earnings DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_commissions_paid DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    last_withdrawal_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_store_wallets_store_id ON store_wallets(store_id);

-- Store Wallet Transactions Table (audit trail for store wallets)
CREATE TABLE store_wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_wallet_id UUID NOT NULL REFERENCES store_wallets(id),
    amount DECIMAL(10, 2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255),
    reference_type VARCHAR(100),
    description TEXT,
    status VARCHAR(50) NOT NULL,
    balance_before DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_store_wallet_transactions_wallet_id ON store_wallet_transactions(store_wallet_id);
CREATE INDEX idx_store_wallet_transactions_created_at ON store_wallet_transactions(created_at);
CREATE INDEX idx_store_wallet_transactions_type ON store_wallet_transactions(transaction_type);

-- Payment Retry Logs Table (debugging failed payments)
CREATE TABLE payment_retry_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id UUID NOT NULL REFERENCES payment_transactions(id),
    error_code VARCHAR(50),
    error_message TEXT,
    response_body TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_retry_logs_payment_id ON payment_retry_logs(payment_transaction_id);
CREATE INDEX idx_payment_retry_logs_created_at ON payment_retry_logs(created_at);
