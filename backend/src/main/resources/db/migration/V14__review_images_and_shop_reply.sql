ALTER TABLE product_reviews ADD COLUMN shop_reply VARCHAR(1000);
ALTER TABLE product_reviews ADD COLUMN replied_at TIMESTAMPTZ;
ALTER TABLE product_reviews ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE review_images (
    review_id BIGINT NOT NULL REFERENCES product_reviews(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position BETWEEN 0 AND 4),
    url VARCHAR(500) NOT NULL,
    PRIMARY KEY (review_id, position),
    UNIQUE (review_id, url)
);
CREATE INDEX idx_reviews_product_rating_created ON product_reviews(product_id, rating, created_at DESC, id DESC);
CREATE FUNCTION notify_review_reply() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.shop_reply IS NOT NULL AND OLD.shop_reply IS NULL THEN
        INSERT INTO notifications(user_id, event_key, title, body, link)
        VALUES (NEW.user_id, 'review-reply:' || NEW.id, 'Cửa hàng đã phản hồi đánh giá',
            'Đánh giá sản phẩm của bạn đã nhận được phản hồi.', '/products/' || NEW.product_id || '#reviews')
        ON CONFLICT (user_id, event_key) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_review_reply_notification AFTER UPDATE OF shop_reply ON product_reviews
FOR EACH ROW EXECUTE FUNCTION notify_review_reply();
