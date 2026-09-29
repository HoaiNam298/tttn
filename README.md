# TTTN ShopApp

Ứng dụng thương mại điện tử gồm Angular frontend và Spring Boot backend.

## Yêu cầu

- Node.js 20+
- Java 17
- Maven 3.9+

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
