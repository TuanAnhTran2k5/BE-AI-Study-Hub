# 📖 (BE) SpringBoot Project Coding Convention

## 📂 Cấu trúc thư mục

| **Thư mục / File**   | **Vai trò chính**                                                        |
| -------------------- | ------------------------------------------------------------------------ |
| **config/**          | Cấu hình chung (Security, CORS, Swagger, DataSource…).                   |
| **controller/**      | REST API endpoints (lớp giao tiếp với client, request/response mapping). |
| **dto/**             | Data Transfer Objects (Request/Response).                                |
| **entity/**          | JPA Entities ánh xạ với bảng DB.                                         |
| **exception/**       | Xử lý lỗi, `GlobalExceptionHandler`, custom exception.                   |
| **repository/**      | Tầng giao tiếp DB (`JpaRepository`, `CrudRepository`).                   |
| **service/**         | Business logic, xử lý dữ liệu.                                           |
| **mapper/**          | Map dữ liệu giữa Entity ↔ DTO (dùng MapStruct hoặc thủ công).            |
| **util/**            | Các hàm tiện ích (validation, date utils,…).                             |
| **security/**        | Xác thực/ủy quyền (JWT, Spring Security).                                |
| **Application.java** | File root chạy ứng dụng Spring Boot.                                     |
| **resources/**       | Cấu hình (`application.yml`), template, static files.                    |
| **test/**            | Unit test & integration test.                                            |
| **deploy.sh**        | Script build/deploy (Maven/Gradle).                                      |
| **.env/**            | File môi trường (DB_URL, SECRET_KEY,…), không commit.                    |

---

## 🔤 Quy tắc đặt tên

- **Package**: lowercase, không underscore

  - Ví dụ: `com.company.project.controller`

- **Class**: PascalCase

  - Ví dụ: `UserController`, `AuthService`

- **Interface**: PascalCase, có hậu tố rõ ràng

  - Ví dụ: `UserRepository`, `IUserService`

- **Biến, hàm**: camelCase

  - Ví dụ: `findByEmail`, `getUserName`

- **Hằng số**: UPPER_CASE

  - Ví dụ: `DEFAULT_PAGE_SIZE`, `API_KEY`

- **Entity**: PascalCase, số ít (map với bảng DB)

  - Ví dụ: `User`, `ProductOrder`

- **DTO**: PascalCase + hậu tố `Dto`

  - Ví dụ: `UserResponseDto`, `LoginRequestDto`

- **Enum**: PascalCase + giá trị UPPER_CASE
  ```java
  public enum UserRole {
      ADMIN,
      USER
  }
  ```

## ⚙️ Config & Constants

### 🔑 Config

- Dùng **application.yml** thay vì `.properties` để dễ đọc & tổ chức.
- Secrets (DB, JWT, API Key) lưu trong `.env` hoặc **Spring Cloud Config**, không commit lên Git.
- Ví dụ:
  ```yaml
  spring:
    datasource:
      url: ${DB_URL}
      username: ${DB_USER}
      password: ${DB_PASS}
    jpa:
      hibernate:
        ddl-auto: update
      show-sql: true
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600000
  ```

### 🎯Constants

- Đặt trong package `constant`
- Quy tắc đặt tên: `UPPER_CASE`


# 📘(FE) React Project Coding Convention

## 📂 Cấu trúc thư mục

| **Thư mục / File** | **Vai trò chính** |
|---------------------|-------------------|
| **components/**    | Các UI components tái sử dụng, tổ chức theo **Atomic Design** (atoms, molecules, organisms, templates). |
| **config/**        | Cấu hình chung cho ứng dụng (env, constants, theme, router config,…). |
| **hook/**          | Custom React hooks để tái sử dụng logic (`useAuth`, `useFetch`, …). |
| **layout/**        | Layout tổng thể (Header, Sidebar, Footer, AppLayout,…). |
| **model/**         | Mock data / fake data phục vụ phát triển & prototype UI (không dùng cho production). |
| **page/**          | Các trang chính (Home, Login, Dashboard…), mapping trực tiếp với route. |
| **redux/**         | Quản lý state với **Redux Toolkit**: `store/`, `slice/`, `middleware/`, `persist/`. |
| **service/**       | Tầng service giao tiếp với API/backend: axios instance, API methods. |
| **utils/**         | Các hàm tiện ích (formatDate, validateEmail, debounce,…). |
| **App.jsx**        | File root chứa khung ứng dụng (routing, provider, layout). |
| **index.css**      | CSS global áp dụng cho toàn app (reset, global style, theme). |
| **main.jsx**       | Điểm khởi đầu, render React vào DOM root, khởi tạo provider (Redux, Router…). |
| **deploy.sh**      | Script tự động hóa **deploy** lên server. |
| **.env/**          | File môi trường (API_URL, TOKEN_KEY,…), không commit lên git (nếu chứa secret). |

---
## 🔤 2. Quy tắc đặt tên

- **Component (React)**: PascalCase  
  - Ví dụ: `UserProfile.tsx`, `LoginForm.tsx`

- **Hook (Custom hook)**: bắt đầu bằng `useXxx`  
  - Ví dụ: `useAuth.ts`, `useFetch.ts`

- **File TypeScript thường**: camelCase  
  - Ví dụ: `formatDate.ts`, `validateEmail.ts`

- **Folder**: lowercase hoặc kebab-case  
  - Ví dụ: `user-profile/`, `auth/`

- **Biến, hàm**: camelCase  
  - Ví dụ: `handleSubmit`, `userName`

- **Hằng số**: UPPER_CASE  
  - Ví dụ: `API_BASE_URL`, `DEFAULT_TIMEOUT`

- **Kiểu dữ liệu / Interface / Type**: PascalCase, có prefix rõ ràng  
  - Interface: `IUser`, `IProduct`  
  - Type: `UserRole`, `AuthResponse`  
  - Enum: PascalCase + giá trị UPPER_CASE  
    ```ts
    enum UserRole {
      ADMIN = "ADMIN",
      USER = "USER",
    }
    ```

- **Generic Type**: đặt tên ngắn gọn, chữ in hoa  
  - Ví dụ: `T`, `K`, `V`, hoặc có ý nghĩa hơn: `TUser`, `TResponse`
## 🎨 Biến màu chung

```css
--primary-color:   ;
--secondary-color: ;
--default-color:   ;
--neutral-color:   ;
```
# 🌿 GitHub Convention

## 1. Branch Naming Convention

- Dùng **kebab-case** (chữ thường, dấu `-` ngăn cách).
- Prefix theo loại nhánh:

| Loại nhánh                       | Cách đặt tên        | Ví dụ                         |
| -------------------------------- | ------------------- | ----------------------------- |
| **Feature** (tính năng mới)      | `feature/<tên>`     | `feature/user-authentication` |
| **Fix** (sửa bug)                | `fix/<tên>`         | `fix/null-pointer-login`      |
| **Hotfix** (sửa gấp production)  | `hotfix/<tên>`      | `hotfix/jwt-expiry-bug`       |
| **Release** (chuẩn bị phát hành) | `release/<version>` | `release/1.0.0`               |

👉 Tạo nhánh mới:

```bash
git checkout -b feature/user-authentication
```

### 2. Commit Message Convention

Commit message cần **rõ ràng, ngắn gọn** và có **prefix theo chuẩn**:

| Prefix       | Ý nghĩa                                        | Ví dụ                                         |
| ------------ | ---------------------------------------------- | --------------------------------------------- |
| **feat**     | Thêm tính năng mới                             | `feat: implement JWT authentication`          |
| **fix**      | Sửa lỗi                                        | `fix: resolve NPE in user service`            |
| **docs**     | Cập nhật tài liệu (README, wiki, ...)          | `docs: update API usage guide`                |
| **style**    | Format code, convention, không ảnh hưởng logic | `style: reformat code with Google Java Style` |
| **refactor** | Thay đổi code nhưng không thêm tính năng/bug   | `refactor: extract email validation util`     |
| **test**     | Thêm/sửa test                                  | `test: add unit test for AuthService`         |
| **chore**    | Cập nhật tool, build, CI/CD, dependencies      | `chore: upgrade Spring Boot to 3.3.0`         |

---

### 3. Best Practices

- Một commit = **một thay đổi logic rõ ràng**.
- Không commit code **chưa chạy được**.
- Tránh commit file build/IDE: `target/`, `.idea/`, `*.iml`, …
- Luôn viết commit message bằng **tiếng Anh** (ngắn gọn, dễ hiểu).

```
---
👉 RESOURCES:


