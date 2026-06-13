# PC Master BE

Backend monolith cho he thong E-commerce PC + Build & Bottleneck (Spring Boot).

## Package structure

```
com.edu.pcmaster
├── controllers
├── services
├── repositories
├── models
├── dto
├── security
├── config
└── common
```

## Features

- JWT authentication
- Product CRUD (admin) + public listing
- Supplier + purchase order + receive stock
- FIFO order processing
- PC build with compatibility lookup
- Bottleneck lookup
- Cloudinary upload for media
- Simple chatbot from FAQ / product search

## Configuration

Update values in `src/main/resources/application-dev.properties`:

- `spring.datasource.*`
- `app.jwt.secret`, `app.jwt.expiration-ms`
- `cloudinary.*`

**Specs JSON fields** (used for compatibility):

- `component_type`: `CPU|GPU|MAINBOARD|RAM|PSU|CASE|STORAGE|COOLER`
- `socket`, `ram_type` (for CPU/Mainboard/RAM)
- `tdp` (integer watt)

## Hướng dẫn chạy Backend (Spring Boot)

### 1. Chuẩn bị Cơ sở dữ liệu (PostgreSQL với PGVector)
Dự án sử dụng cơ sở dữ liệu PostgreSQL mở rộng thêm extension `vector` (PGVector) để phục vụ tính năng tìm kiếm ngữ nghĩa bằng AI.

Bạn có thể chạy nhanh qua Docker:
```bash
docker run --name postgres -e POSTGRES_DB=pcmaster -e POSTGRES_PASSWORD=123456 -p 5432:5432 -d pgvector/pgvector:pg16
```

### 2. Cấu hình ứng dụng
Cấu hình kết nối DB và các thông số khác tại file [application-dev.properties](file:///c:/Users/tienc/Documents/DATN/PCMaster_Backend/src/main/resources/application-dev.properties):
- `spring.datasource.url=jdbc:postgresql://localhost:5432/pcmaster`
- `spring.datasource.username=postgres`
- `spring.datasource.password=123456`

### 3. Chạy ứng dụng
Dùng Maven Wrapper đi kèm để khởi động server Spring Boot:
```bash
# Windows (cmd/PowerShell)
./mvnw spring-boot:run
```

---

## Hướng dẫn khởi tạo và nhập dữ liệu (Database & Inventory)

Tất cả các file dữ liệu SQL được lưu trữ tại thư mục [Database/](file:///c:/Users/tienc/Documents/DATN/PCMaster_Backend/Database).

### 1. Khôi phục dữ liệu ban đầu (Database Restore)
Để khôi phục toàn bộ cấu trúc bảng và dữ liệu mẫu hiện tại của dự án:
Sử dụng công cụ PostgreSQL Client (DBeaver, pgAdmin) hoặc chạy lệnh dưới đây qua Docker:
```bash
docker exec -i postgres psql -U postgres -d pcmaster < Database/pcmaster_backup.sql
```

### 2. Nhập dữ liệu tồn kho hàng loạt (Import Inventory)
Dự án quản lý hàng tồn kho theo lô (FIFO). Bạn có thể chạy các script SQL để mô phỏng quá trình nhập hàng từ nhà phân phối:

- **Nhập 40 đơn vị tồn kho cho tất cả sản phẩm từ nhà cung cấp 'AIO'**:
  Chạy file [import_aio.sql](file:///c:/Users/tienc/Documents/DATN/PCMaster_Backend/Database/import_aio.sql) trong Database tool của bạn. Script này sẽ:
  - Tự động kiểm tra hoặc tạo mới nhà phân phối tên 'AIO'.
  - Tạo đơn nhập hàng (Purchase Order) với trạng thái `RECEIVED`.
  - Tạo các lô hàng (Inventory Batches) tương ứng và cập nhật số lượng tồn kho của mỗi sản phẩm lên 40.

- **Nhập 20 đơn vị tồn kho cho riêng các sản phẩm SSD từ nhà cung cấp 'AIO'**:
  Chạy file [import_ssd_aio.sql](file:///c:/Users/tienc/Documents/DATN/PCMaster_Backend/Database/import_ssd_aio.sql) trong Database tool của bạn. Script này sẽ tạo lô nhập hàng bổ sung cho riêng nhóm linh kiện ổ cứng SSD.

---

## Test & Endpoints

### Chạy Unit Test
```bash
./mvnw test
```

### Các Endpoints chính
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `GET /api/products` - Tìm kiếm, hiển thị danh sách sản phẩm
- `GET /api/products/{id}` - Chi tiết sản phẩm
- `GET /api/categories` - Danh mục sản phẩm
- `POST /api/admin/products` - Tạo mới sản phẩm (Admin)
- `POST /api/admin/purchase-orders` - Tạo đơn nhập kho mới (Admin)
- `PUT /api/admin/purchase-orders/{id}/receive` - Xác nhận nhận hàng và tạo lô hàng tồn kho (Admin)
- `POST /api/orders` - Đặt mua hàng (Trừ kho FIFO tự động)
- `GET /api/builds` - Lấy cấu hình máy tính
- `POST /api/chat` - Chatbot tư vấn ngữ nghĩa (RAG)
- `POST /api/admin/media/upload` - Upload ảnh lên Cloudinary
