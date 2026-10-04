INSERT INTO categories(name)
VALUES
    ('Máy tính bảng'),
    ('Đồng hồ thông minh'),
    ('Tai nghe'),
    ('Màn hình'),
    ('Bàn phím'),
    ('Chuột máy tính'),
    ('Thiết bị mạng')
ON CONFLICT (name) DO NOTHING;

WITH catalog(category_name, product_prefix, image_key, base_price, description) AS (
    VALUES
        ('Điện thoại', 'Điện thoại ShopPhone', 'phone', 4990000::NUMERIC, 'Điện thoại thông minh dành cho học tập, làm việc và giải trí hàng ngày.'),
        ('Laptop', 'Laptop ShopBook', 'laptop', 12990000::NUMERIC, 'Laptop hiệu năng ổn định, thiết kế gọn nhẹ và phù hợp nhiều nhu cầu sử dụng.'),
        ('Phụ kiện', 'Phụ kiện công nghệ', 'accessory', 190000::NUMERIC, 'Phụ kiện công nghệ tiện dụng, dễ kết nối và tương thích với nhiều thiết bị.'),
        ('Máy tính bảng', 'Máy tính bảng ShopTab', 'tablet', 5990000::NUMERIC, 'Máy tính bảng màn hình sắc nét, pin lâu và thuận tiện khi di chuyển.'),
        ('Đồng hồ thông minh', 'Đồng hồ ShopWatch', 'watch', 1490000::NUMERIC, 'Đồng hồ thông minh hỗ trợ theo dõi sức khỏe và nhận thông báo nhanh.'),
        ('Tai nghe', 'Tai nghe ShopSound', 'headphone', 490000::NUMERIC, 'Tai nghe có âm thanh rõ ràng, đeo thoải mái và thời lượng pin tốt.'),
        ('Màn hình', 'Màn hình ShopView', 'monitor', 2990000::NUMERIC, 'Màn hình hiển thị màu sắc rõ nét, phù hợp làm việc và giải trí.'),
        ('Bàn phím', 'Bàn phím ShopKey', 'keyboard', 590000::NUMERIC, 'Bàn phím có độ phản hồi tốt, bố cục dễ sử dụng và thiết kế bền bỉ.'),
        ('Chuột máy tính', 'Chuột ShopClick', 'mouse', 290000::NUMERIC, 'Chuột máy tính chính xác, cầm nắm thoải mái và kết nối ổn định.'),
        ('Thiết bị mạng', 'Thiết bị mạng ShopNet', 'network', 790000::NUMERIC, 'Thiết bị mạng cho kết nối ổn định, vùng phủ tốt và cài đặt đơn giản.')
)
INSERT INTO products(name, price, thumbnail, description, category_id, stock)
SELECT
    catalog.product_prefix || ' ' || LPAD(series.number::TEXT, 2, '0'),
    catalog.base_price + (series.number - 1) * 175000,
    'https://placehold.co/800x600/e2e8f0/0f766e?text=' || catalog.image_key || '-' || LPAD(series.number::TEXT, 2, '0'),
    catalog.description || ' Phiên bản demo số ' || LPAD(series.number::TEXT, 2, '0') || '.',
    categories.id,
    15 + (series.number * 7 % 86)
FROM catalog
JOIN categories ON categories.name = catalog.category_name
CROSS JOIN generate_series(1, 20) AS series(number);
