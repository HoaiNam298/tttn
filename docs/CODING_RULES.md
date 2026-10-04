# Quy chuẩn cấu trúc và mã nguồn ShopApp

Tài liệu này là quy chuẩn bắt buộc cho source mới. Cấu trúc được kế thừa từ
ShopApp cũ để việc chuyển từng module dễ đối chiếu, nhưng sửa các tên sai và loại
bỏ cách tổ chức không nhất quán trong source cũ.

## 1. Quy tắc chung

- Không tự tạo package hoặc thư mục mới ngoài cây chuẩn bên dưới. Nếu cần bổ sung,
  phải cập nhật tài liệu này trước.
- Mỗi class, component, service hoặc model chính nằm trong file riêng.
- Không dồn khai báo, điều kiện hoặc xử lý nghiệp vụ lên một dòng.
- `if`, `else`, vòng lặp và callback luôn có dấu ngoặc nhọn.
- Không dùng wildcard import; xóa mọi import không sử dụng.
- Tên class/interface dùng PascalCase; biến, method và property dùng camelCase.
- Mỗi file kết thúc bằng newline và không có trailing whitespace.
- Không commit `.env`, secret, thư mục build, dependency hoặc cache IDE.

## 2. Cây thư mục backend

Backend kế thừa cách phân lớp của source cũ. Tên package dùng chữ thường, danh từ
số nhiều cho nhóm chứa nhiều loại đối tượng.

```text
backend/src/main/java/com/project/shopapp/
├── components/        # Thành phần dùng chung, mapper, localization
├── configuration/     # Spring Security, CORS và cấu hình bean
├── controller/        # REST controller
├── dtos/              # Request DTO từ client
├── exceptions/        # Exception và global exception handler
├── filters/           # JWT/security servlet filter
├── model/             # JPA entity và enum
├── repositories/      # Spring Data repository
├── responses/         # DTO trả về client
├── services/          # Interface định nghĩa nghiệp vụ
│   └── impl/          # Implementation của service
└── utils/             # Tiện ích thuần, không chứa nghiệp vụ
```

Các tên từ source cũ không được tiếp tục sử dụng:

- `ultils` phải đổi thành `utils`.
- `services/Impl` phải đổi thành `services/impl`.
- Không đặt security configuration trong `components`.

### Trách nhiệm backend

- `controller`: nhận request, gọi service, tạo HTTP response; không viết nghiệp vụ.
- `dtos`: chỉ chứa dữ liệu request và Bean Validation.
- `responses`: chỉ chứa dữ liệu response và mapper cần thiết.
- `services`: interface cho use case của ứng dụng.
- `services/impl`: transaction và xử lý nghiệp vụ.
- `repositories`: chỉ truy cập dữ liệu.
- `filters`: filter xác thực; xử lý JWT thuần có thể đặt trong `components`.
- `configuration`: khai báo bean và chuỗi cấu hình Spring.
- API tạo mới trả `201 Created`; xóa thành công trả `204 No Content`.
- Không trả JPA entity trực tiếp từ controller.

## 3. Cây thư mục frontend

Frontend kế thừa cách chia component của source cũ, nhưng dùng tên Angular chuẩn và
sửa lỗi chính tả `intercepters` thành `interceptors`.

```text
frontend/src/app/
├── components/
│   ├── admin/
│   │   ├── category/
│   │   └── product/
│   ├── layout/
│   │   ├── header/
│   │   └── footer/
│   ├── login/
│   ├── product-detail/
│   ├── product-list/
│   └── shared/
├── dtos/              # Payload gửi lên API
├── environments/      # API URL theo môi trường
├── guards/            # Route guards
├── interceptors/      # HTTP interceptors
├── models/            # Domain model dùng trong giao diện
├── responses/         # Page/API response models
├── services/          # HTTP và state services
├── app.component.*
├── app.config.ts
└── app.routes.ts
```

### Trách nhiệm frontend

- `components`: chỉ xử lý hiển thị, input/output và tương tác người dùng.
- `services`: gọi API và quản lý state; component không gọi `HttpClient` trực tiếp.
- `dtos`: kiểu dữ liệu gửi lên backend.
- `responses`: wrapper response như pagination hoặc authentication response.
- `models`: model nghiệp vụ như `Product`, `Category`, `User`.
- `guards` và `interceptors`: mỗi guard/interceptor nằm trong file riêng.
- Component có HTML phải dùng `templateUrl`; không viết template nhiều dòng trong
  file TypeScript.
- Method và service HTTP phải khai báo kiểu trả về rõ ràng.
- Object/interface lồng nhau phải tách thành kiểu có tên.
- Form nhập liệu dùng Reactive Forms và có validation, loading, empty/error state.

## 4. Format backend

- Java 17, Spotless và Google Java Format AOSP.
- Constructor hoặc method có nội dung phải trình bày nhiều dòng.
- Record có nhiều trường trình bày mỗi trường trên dòng phù hợp.
- Thay đổi endpoint phải có test controller hoặc service tương ứng.

```bash
cd backend
mvn spotless:apply
mvn clean verify
```

## 5. Format frontend

- TypeScript, HTML và SCSS được format bằng Prettier.
- Không dùng one-line `if`, method nghiệp vụ hoặc inline HTML template.
- Các block logic khác nhau phải cách nhau bằng dòng trống.
- SCSS dùng class có ý nghĩa, không dùng style inline trong HTML.

```bash
cd frontend
npm run format
npm run format:check
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

## 6. Mapping source hiện tại sang cây chuẩn

Backend:

| Hiện tại | Cây chuẩn |
|---|---|
| `config` | `configuration` |
| `dto` | request chuyển vào `dtos`, response chuyển vào `responses` |
| `exception` | `exceptions` |
| `repository` | `repositories` |
| `security/JwtAuthenticationFilter` | `filters` |
| `security/JwtService` | `components` |
| class trong `service` | interface trong `services`, implementation trong `services/impl` |

Frontend:

| Hiện tại | Cây chuẩn |
|---|---|
| `admin` | `components/admin` |
| `auth/login` | `components/login` |
| `catalog` | `components/product-list` và `components/product-detail` |
| `core/catalog.service` | `services/catalog.service` |
| `core/auth.service` | `services/auth.service` |
| `core/admin.guard` | `guards/admin.guard` |
| `core/auth.interceptor` | `interceptors/auth.interceptor` |
| `core/models` | tách vào `models`, `dtos` và `responses` |

## 7. Điều kiện hoàn thành task

- File mới nằm đúng cây chuẩn và đúng trách nhiệm.
- Không còn import tham chiếu tới cây thư mục cũ của module vừa sửa.
- Formatter, test và build của FE/BE đều pass.
- `git diff --check` không báo lỗi.
- `git status` không chứa secret hoặc artifact.
