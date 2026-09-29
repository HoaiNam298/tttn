# TTTN ShopApp

Ứng dụng thương mại điện tử gồm Angular frontend và Spring Boot backend.

## Yêu cầu

- Node.js 20+
- Java 17
- Maven 3.9+
- Docker Desktop

## Khởi động PostgreSQL

```bash
docker compose up -d
docker compose ps
```

PostgreSQL chạy trong container `shopapp-postgres` và được mở tại `localhost:5433` để tránh xung đột với PostgreSQL cài trên máy. Dữ liệu được lưu trong Docker volume.

Để dừng database:

```bash
docker compose stop
```

## Chạy frontend

```bash
cd frontend
npm install
npm start
```

Frontend chạy tại `http://localhost:4200`.

## Chạy backend

```bash
cd backend
mvn spring-boot:run
```

Backend chạy tại `http://localhost:8088`. Kiểm tra API tại `http://localhost:8088/api/v1/health`.

Các API nền tảng:

- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
