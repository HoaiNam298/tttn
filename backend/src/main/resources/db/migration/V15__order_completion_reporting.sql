ALTER TABLE orders ADD COLUMN completed_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN completion_time_estimated BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE orders o SET completed_at = COALESCE(
    (SELECT MAX(h.occurred_at) FROM order_status_history h WHERE h.order_id = o.id AND h.status = 'COMPLETED'),
    o.updated_at),
    completion_time_estimated = NOT EXISTS (
        SELECT 1 FROM order_status_history h WHERE h.order_id = o.id AND h.status = 'COMPLETED' AND NOT h.imported)
WHERE o.status = 'COMPLETED';
ALTER TABLE orders ADD CONSTRAINT ck_completed_order_timestamp
    CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL));
CREATE INDEX idx_orders_completed_at ON orders(completed_at, id) WHERE status = 'COMPLETED';
