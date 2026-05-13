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

## Run

```
# PowerShell
./mvnw spring-boot:run
```

## Test

```
# PowerShell
./mvnw test
```

## Core endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/categories`
- `GET /api/brands`
- `POST /api/admin/products` (multipart: `data` JSON + optional `thumbnail` file)
- `PUT /api/admin/products/{id}`
- `DELETE /api/admin/products/{id}`
- `GET /api/admin/categories`
- `POST /api/admin/categories`
- `PUT /api/admin/categories/{id}`
- `DELETE /api/admin/categories/{id}`
- `GET /api/admin/brands`
- `POST /api/admin/brands`
- `PUT /api/admin/brands/{id}`
- `DELETE /api/admin/brands/{id}`
- `GET /api/admin/suppliers`
- `POST /api/admin/suppliers`
- `POST /api/admin/purchase-orders`
- `GET /api/admin/purchase-orders`
- `GET /api/admin/purchase-orders/{id}`
- `PUT /api/admin/purchase-orders/{id}/receive`
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `GET /api/builds`
- `POST /api/builds`
- `GET /api/builds/{id}`
- `POST /api/builds/{id}/items`
- `PUT /api/builds/{id}/items/{itemId}`
- `DELETE /api/builds/{id}/items/{itemId}`
- `GET /api/builds/{id}/compatible-components?type=MAINBOARD`
- `GET /api/bottleneck?cpuId=&gpuId=&res=`
- `POST /api/chat`
- `POST /api/admin/media/upload`

## Sample image seeder

Set these in `src/main/resources/application-dev.properties` (or env vars) to upload sample images to Cloudinary and seed DB:

- `app.seed.sample-images.enabled=true`
- `app.seed.sample-images.root=Sample_image`

The seeder uploads:
- `Sample_image/Product_Image/<Category>` -> `PCMAster_Storage/Product_thumbnails/<Category>`
- `Sample_image/Brands_Logo/<Category>` -> `PCMAster_Storage/Brands_Logos/<Category>`

Brand responses now include `logoUrl`.

## Core seeder

Enable with `app.seed.core.enabled=true` (enabled by default in `application-dev.properties`).

Creates users:
- `admin` / `Admin@123`
- `customer` / `Customer@123`
- `buyer` / `Buyer@123`

Creates sample suppliers and assigns each supplier 5-10 random brands.
