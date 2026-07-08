ALTER TABLE stores ADD COLUMN owner_id UUID REFERENCES users(id);
