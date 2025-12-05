# 🎓 Study-S: Mạng Xã Hội Học Tập Cho Sinh Viên

## 📖 1. Tổng Quan Về Đề Tài

### ❓ Bối cảnh & Lý do chọn đề tài
Trong môi trường đại học hiện nay, sinh viên phải quản lý khối lượng kiến thức lớn và lịch trình dày đặc. Tuy nhiên, việc sử dụng nhiều ứng dụng rời rạc (Lịch riêng, Chat riêng, Drive riêng...) gây ra sự phân tán dữ liệu, bất tiện và giảm hiệu suất học tập.

**Study-S** ra đời nhằm giải quyết vấn đề này bằng cách xây dựng một **hệ sinh thái học tập thu nhỏ (All-in-one)**, tích hợp mạng xã hội, quản lý thời gian và làm việc nhóm vào một ứng dụng duy nhất.

### 🎯 Mục tiêu & Phạm vi
* **Mục tiêu:** Tạo môi trường kết nối, thảo luận nhóm, quản lý tài liệu và lịch học dễ dàng.
* **Phạm vi người dùng:** Sinh viên các trường đại học, cao đẳng.
* **Phạm vi kỹ thuật:** Ứng dụng Android, sử dụng dịch vụ đám mây (Firebase, Cloudinary).

### 🔬 Phương pháp nghiên cứu
* **Phân tích yêu cầu:** Xác định nghiệp vụ cốt lõi (Quản lý lịch, Nhóm, Tài liệu).
* **Kiến trúc:** Áp dụng mô hình **MVVM (Model - View - ViewModel)** hiện đại.
* **Thiết kế:** Sử dụng Figma cho UI/UX và sơ đồ UML cho luồng dữ liệu.

---

## ✨ 2. Tính Năng Nổi Bật

### 🌐 Mạng xã hội học tập
* **Newfeed:** Đăng bài viết, hình ảnh, cập nhật tin tức.
* **Tương tác:** Like, Bình luận, Lưu bài viết.
* **Kết nối:** Theo dõi (Follow) bạn bè và người dùng khác.

### 📚 Quản lý học tập
* **📅 Lịch cá nhân:** Tạo sự kiện, deadline, đặt nhắc nhở (Alarm).
* **📂 Thư viện tài liệu:** Upload và lưu trữ tài liệu (PDF, Ảnh...) không giới hạn nhờ tích hợp **Cloudinary**.

### 👥 Làm việc nhóm (Group Study)
* **Quản lý nhóm:** Tạo nhóm mới, duyệt thành viên, tìm kiếm nhóm.
* **Chat Real-time:** Nhắn tin, gửi ảnh/tài liệu trong thời gian thực cho các thành viên nhóm.

---

## 🛠 3. Công Nghệ Sử Dụng

Dự án được xây dựng trên nền tảng công nghệ mới nhất của Google dành cho Android:

| Hạng mục | Công nghệ chi tiết |
| :--- | :--- |
| **Ngôn ngữ** | Kotlin |
| **Giao diện (UI)** | **Jetpack Compose** (Declarative UI) |
| **Kiến trúc** | MVVM (Model - View - ViewModel) |
| **Authentication** | Firebase Auth (Email/Pass, Google Sign-in) |
| **Database** | Firebase Firestore & Realtime Database |
| **Lưu trữ (Storage)** | **Cloudinary** (Tối ưu hóa hình ảnh & tài liệu) |
| **Công cụ** | Android Studio, Figma, Git/GitHub |

---

## 🚀 4. Hướng Dẫn Cài Đặt & Triển Khai

### ⚙️ Yêu cầu hệ thống
* **IDE:** Android Studio (Phiên bản hỗ trợ Jetpack Compose).
* **JDK:** Phiên bản 11 hoặc 17 trở lên.
* **Internet:** Kết nối ổn định.

### Dành cho Developer (Chạy Source Code)
**Clone dự án**
git clone [https://github.com/hahoang43/Study-S.git]
### 📲 Cài đặt nhanh qua mã QR (Dành cho người dùng)

Bạn có thể tải và cài đặt ứng dụng ngay lập tức mà không cần máy tính:

1.  **Bước 1:** Sử dụng **Camera** trên điện thoại Android hoặc ứng dụng **Zalo / QR Scanner** bất kỳ.
2.  **Bước 2:** Quét mã QR bên dưới.
3.  **Bước 3:** Truy cập liên kết hiện ra để tải xuống tệp cài đặt (`.apk`).
4.  **Bước 4:** Nhấn vào file đã tải và chọn **"Cài đặt" (Install)**.

> *Lưu ý: Nếu thiết bị yêu cầu quyền bảo mật, vui lòng chọn "Cho phép cài đặt từ nguồn này" (Allow from this source).*

![QR Code Scan Me]()

