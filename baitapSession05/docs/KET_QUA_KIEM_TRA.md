# Kiểm tra Session05 - DTO Projection

Đã chạy gradlew.bat test bootJar với Java 21: BUILD SUCCESSFUL.

- CoursePaginationTests: 25 test pass.
- CourseEnrollmentApiTests: 14 test pass.
- EntityMappingTests: 14 test pass.
- RepositoryServiceTests: 4 test pass.
- Tổng: 57 test, 0 lỗi/thất bại.

Test với 10 Course COMPLETED, mỗi Course gắn một Instructor khác nhau:

- Đọc Page<Course> rồi lấy tên giảng viên: 12 SQL (1 data + 1 count + 10 Instructor).
- Gọi service dùng JPQL constructor projection: 2 SQL (1 DTO data + 1 count).
- Entity load/fetch khi dùng projection: 0.

JSON mỗi item chỉ còn id, title, status; wrapper vẫn đủ 6 trường. Các dữ liệu thử nghiệm rollback sau test.

Đã kiểm tra query JPQL lọc status trước khi phân trang: ACTIVE mặc định, INACTIVE với title ASC/DESC, page âm, trang kế tiếp, tổng số bản ghi chỉ tính kết quả lọc; status không hợp lệ trả 400, status không có bản ghi trả trang rỗng.

Build và project cache được tạo ngoài project tại `D:\Code\Springboot\.codex-backups\session05-jpql-build` và `session05-jpql-cache` để giữ thư mục nộp dưới 100 file.

Test API kiểm tra PageResponse có đúng 6 trường: items, page, size, totalItems, totalPages, last. Trang đầu có last=false, trang vượt phạm vi có items rỗng và last=true. Không còn trả pageable/sort ra JSON.

Các test dùng database riêng course_management_session05_test và rollback dữ liệu. JSON trong courses-page-example.json là ví dụ minh họa cấu trúc mới, không phải bản ghi lần gọi HTTP mới.
