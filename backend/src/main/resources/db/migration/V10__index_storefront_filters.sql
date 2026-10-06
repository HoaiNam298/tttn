CREATE INDEX idx_products_price_id ON products(price, id);
CREATE INDEX idx_products_category_price_id ON products(category_id, price, id);
CREATE INDEX idx_orders_user_status_created ON orders(user_id, status, created_at DESC);
