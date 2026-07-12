-- Add Service Provider fields to Stores
ALTER TABLE stores ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS ads_urls JSONB;

-- Create Store Followers table for the follow/unfollow feature
CREATE TABLE IF NOT EXISTS store_followers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(store_id, user_id)
);
