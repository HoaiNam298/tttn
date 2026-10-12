-- Replace only the known broken demo URL; preserve merchant-uploaded images.
UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=900&h=900&q=80'
WHERE image_url = 'https://images.unsplash.com/photo-1606420187127-8a26fcf0aa5d?auto=format&fit=crop&w=900&h=900&q=80';
