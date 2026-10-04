UPDATE products AS product
SET thumbnail =
    CASE category.name
        WHEN 'Điện thoại' THEN 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Laptop' THEN 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Phụ kiện' THEN 'https://images.unsplash.com/photo-1625842268584-8f3296236761?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Máy tính bảng' THEN 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Đồng hồ thông minh' THEN 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Tai nghe' THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Màn hình' THEN 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Bàn phím' THEN 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Chuột máy tính' THEN 'https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=800&h=600&q=80'
        WHEN 'Thiết bị mạng' THEN 'https://images.unsplash.com/photo-1606904825846-647eb07f5be2?auto=format&fit=crop&w=800&h=600&q=80'
        ELSE product.thumbnail
    END
FROM categories AS category
WHERE category.id = product.category_id;
