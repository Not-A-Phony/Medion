-- V13__add_featured_to_categories.sql
ALTER TABLE categories ADD COLUMN is_featured BOOLEAN DEFAULT FALSE;

INSERT INTO categories (id, name, slug, type, status, is_featured, created_at, updated_at) VALUES 
(gen_random_uuid(), 'Power Tools', 'power-tools', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Hand Tools', 'hand-tools', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Lumber & Building', 'lumber-building', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Plumbing', 'plumbing', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Electrical', 'electrical', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Paint & Supplies', 'paint', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Hardware & Fasteners', 'hardware-fasteners', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Safety Equipment', 'safety-equipment', 'PRODUCT', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
