# Ứng dụng Đấu giá Trực tuyến (Hollow)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?style=for-the-badge&logo=openjfx&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.11.0-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey?style=for-the-badge)
![Status](https://img.shields.io/badge/status-active-success.svg?style=for-the-badge)

![](assets/banner.png)

## 1. Mô tả bài toán và phạm vi hệ thống
Hollow là một ứng dụng Desktop (JavaFX) mô phỏng một hệ thống đấu giá trực tuyến. Hệ thống cho phép người dùng đăng ký, đăng nhập, duyệt các phiên đấu giá đang diễn ra, đặt giá cho các vật phẩm, và theo dõi trạng thái đấu giá theo thời gian thực. Ngoài ra, người dùng cũng có thể đóng vai trò là người bán để tạo các phiên đấu giá mới và quản lý các vật phẩm mình đang bán. Phạm vi hệ thống bao gồm một ứng dụng Client (JavaFX) giao tiếp với một máy chủ Server (backend cung cấp REST API).

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
*   **Ngôn ngữ lập trình:** Java (yêu cầu JDK 17 trở lên).
*   **Giao diện người dùng (GUI):** JavaFX.
*   **Giao tiếp mạng:** REST API thông qua thư viện Retrofit và Gson.
*   **Quản lý dự án:** Maven.
*   **Môi trường chạy:** Tương thích đa nền tảng (Windows, Linux, macOS).

**Yêu cầu cài đặt:**
*   Đã cài đặt **Java Development Kit (JDK) 17+**. Kiểm tra bằng lệnh: `java -version`.
*   Đã cài đặt **Apache Maven**. Kiểm tra bằng lệnh: `mvn -version`.

## 3. Cấu trúc thư mục và các module chính
Mã nguồn ứng dụng Client được đặt trong `src/main/java/` và chia thành các module:
*   `model`: Chứa các lớp dữ liệu (Request, Response) để ánh xạ với dữ liệu JSON từ API.
*   `network`: Chứa cấu hình mạng (`ApiClient`), các interface gọi API (`AuctionApi`), và lớp lưu trữ token (`TokenStorage`).
*   `service`: Xử lý logic nghiệp vụ và tương tác với API thông qua callback (ví dụ: `AuthService`).
*   `controller`: Chứa các controller của JavaFX xử lý logic giao diện, được chia thành:
    *   `auth`: Xử lý đăng nhập, đăng ký (`LoginPage`, `RegisterPage`, `LandingPage`).
    *   `auction`: Giao diện chi tiết đấu giá, duyệt vật phẩm, tạo phiên đấu giá (`BidderViewPage`, `SellerViewPage`, `CreatePage`, `BrowseAuctionTab`, `MySaleTab`).
    *   `layout`: Quản lý khung giao diện chính (`Framework`).
    *   `navigation`: Quản lý chuyển đổi các màn hình (`SceneManager`).
*   `AuctionLauncher.java`: Lớp khởi chạy ứng dụng (Entry point).

## 4. Hướng dẫn chạy chương trình

Hệ thống hoạt động theo mô hình Server-Client. **Bạn cần phải khởi động Server trước khi chạy Client.**

### Bước 1: Khởi động Server
*(Lưu ý: Bạn cần có mã nguồn của backend/server. Dưới đây là ví dụ khởi chạy nếu Server là một ứng dụng Spring Boot / Java)*
1. Mở terminal/command prompt và di chuyển vào thư mục chứa mã nguồn Server.
2. Chạy ứng dụng Server:
   * **Windows / Linux / macOS (chạy qua Maven):**
     ```bash
     mvn spring-boot:run
     ```
   * Hoặc nếu chạy từ file `.jar` đã build:
     ```bash
     java -jar target/server-app.jar
     ```
3. Đảm bảo Server đang lắng nghe ở cổng mặc định `8080` (dựa trên cấu hình `http://localhost:8080/` trong `ApiClient`).

### Bước 2: Khởi động Client (Ứng dụng Hollow)
1. Mở một terminal/command prompt khác.
2. Di chuyển vào thư mục gốc của dự án Client (`Hollow`).
3. Biên dịch dự án:
   * **Windows / Linux / macOS:**
     ```bash
     mvn clean compile
     ```
4. Khởi chạy ứng dụng JavaFX:
   * Nếu có sử dụng `javafx-maven-plugin` trong `pom.xml`:
     ```bash
     mvn javafx:run
     ```
   * Hoặc chạy trực tiếp lớp main thông qua Maven:
     ```bash
     mvn exec:java -Dexec.mainClass="AuctionLauncher"
     ```

## 5. Danh sách chức năng đã hoàn thành
*   [x] **Đăng ký tài khoản:** Cho phép người dùng mới tạo tài khoản với tên đăng nhập và mật khẩu.
*   [x] **Đăng nhập:** Xác thực người dùng và lưu trữ token (Access/Refresh Token).
*   [x] **Duyệt phiên đấu giá (Browse Auctions):** Xem danh sách các vật phẩm đang được đấu giá (hỗ trợ phân trang).
*   [x] **Xem chi tiết đấu giá (Bidder View):** Xem thông tin vật phẩm, giá hiện tại, bước giá và người ra giá cao nhất.
*   [x] **Tạo phiên đấu giá (Create Auction):** Thêm vật phẩm mới, thiết lập thời gian, giá khởi điểm, bước giá, và giá Mua ngay (Buy it now).
*   [x] **Quản lý vật phẩm đã đăng (My Sale):** Xem danh sách và theo dõi trạng thái các vật phẩm mình đang bán.
*   [x] **Xem chi tiết với vai trò người bán (Seller View):** Xem tiến độ đấu giá của vật phẩm mình đăng kèm biểu đồ (LineChart) lịch sử đặt giá.

## 6. Liên kết đính kèm
*   **Báo cáo PDF:** [Link Google Drive báo cáo PDF](#) *(Vui lòng cập nhật link)*
*   **Video Demo:** [Link Google Drive video demo](#) *(Vui lòng cập nhật link)*

## 7. Sơ đồ lớp (UML Diagram)
Sơ đồ lớp chi tiết của hệ thống được lưu trữ trong file `project_diagram.puml`. Bạn có thể xem hoặc kết xuất (render) sơ đồ này bằng công cụ [PlantUML](https://plantuml.com/).
