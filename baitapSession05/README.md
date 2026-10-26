# Session05 - Bài 1 đến Bài 4: Phân trang, JPQL và DTO Projection

Project Java 21, Spring Boot 4.1.1, Spring Data JPA và PostgreSQL. Các bài dùng chung project. API **GET /courses** trả `ApiResponse<PageResponse<CourseResponseV2>>`, lọc theo status bằng JPQL và mặc định lấy khóa học ACTIVE.

Bài 4 chỉ thêm CourseResponseV2.java, rồi sửa Repository/Service/Controller để query trực tiếp DTO. `PageResponse<T>` của bài trước vẫn được dùng chung. Trường Java `boolean isLast` có getter `isLast()`, nên tên trong JSON là **last**, giống ảnh mẫu của đề.

Đã kiểm tra: **57/57 JUnit tests pass**, gồm lọc status, JSON của PageResponse và số query khi dùng projection. Xem `docs/KET_QUA_KIEM_TRA.md` và JSON minh họa tại `docs/courses-page-example.json`.

## Chạy trên máy hiện tại

1. Mở `D:\Code\Springboot\baitapSession05` bằng IntelliJ IDEA, Load Gradle Project.
2. Chọn Project SDK và Gradle JVM là **JDK 21**.
3. Chạy `BaitapSession05Application` với working directory là thư mục gốc project.
4. Gọi `http://localhost:8080/courses?page=0&size=2&sortBy=title&direction=ASC`.

PostgreSQL cần đang chạy tại localhost:5432. Database của ứng dụng là `course_management_session05`. Trên máy hiện tại, thông tin đăng nhập nằm ngoài project tại `../.local-config/baitapSession05.properties`, nên không nằm trong thư mục upload GitHub. Máy khác vẫn có thể dùng `config/application-local.properties` theo hướng dẫn bên dưới.

Chạy bằng PowerShell tại thư mục project:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat bootRun
```

## Tham số API

| Query parameter | Mặc định | Xử lý |
| --- | --- | --- |
| `page` | `0` | Đánh số từ 0; số âm được tự chỉnh thành 0 trong service |
| `size` | `10` | Phải lớn hơn 0; giá trị không hợp lệ trả HTTP 400 |
| `sortBy` | `id` | Có thể bỏ qua hoặc để rỗng; hỗ trợ `id`, `title`, `status`, `instructorId` |
| `direction` | `DESC` | Kiểu `Sort.Direction`; nhận `ASC` hoặc `DESC` |
| `status` | `ACTIVE` | Kiểu CourseStatus; nhận ACTIVE, INACTIVE, COMPLETED, CANCELLED |

Trong Spring MVC dùng `@RequestParam(defaultValue = "ACTIVE") CourseStatus status` để lấy query parameter, tương ứng yêu cầu ghi `@QueryParam` trong đề. Status không hợp lệ trả HTTP 400.

`instructorId` được ánh xạ thành thuộc tính `instructor.id` của entity. Nếu nhiều bản ghi trùng giá trị sắp xếp, service sắp tiếp theo ID cùng chiều để giữ ranh giới các trang ổn định.

Tên sort không được hỗ trợ, sai kiểu số, direction không hợp lệ hoặc offset vượt giới hạn phân trang của JPA đều nhận **400** với `success=false`, `message` và `data=null`. Trang vượt quá trang cuối vẫn nhận **200** với `content=[]`.

## Các file chính của bài

```text
src/main/java/com/example/baitapsession05/
    BaitapSession05Application.java
    controller/CourseController.java
    service/CourseService.java
    dto/CourseResponse.java
    dto/CourseResponseV2.java
    repository/CourseRepository.java
    response/ApiResponse.java
    response/PageResponse.java
    exception/BadRequestException.java
    exception/GlobalExceptionHandler.java
src/test/java/com/example/baitapsession05/CoursePaginationTests.java
postman/Session05-Bai1.postman_collection.json
```

### Controller

`@RequestParam` tiếp nhận 5 query parameter với các giá trị mặc định theo đề. Controller gọi service và trả:

```java
ResponseEntity<ApiResponse<PageResponse<CourseResponseV2>>>
```

### Service

Chữ ký phương thức theo yêu cầu:

```java
PageResponse<CourseResponseV2> getPagedCoursesByStatus(int page, int size, String sortBy, Sort.Direction direction, CourseStatus status)
```

Luồng xử lý:

1. `Math.max(page, 0)` để chuẩn hóa số trang âm.
2. Kiểm tra size, offset và tên trường được phép sắp xếp.
3. `Sort.by(direction, sortField)` mô tả thứ tự.
4. `PageRequest.of(safePage, size, sort)` tạo đối tượng `Pageable`.
5. `courseRepository.findAllByStatusV2(status, pageable)` trả trực tiếp `Page<CourseResponseV2>`. Query chỉ chọn id, title và status. TotalItems/totalPages chỉ tính các khóa học khớp status.
6. Tạo `PageResponse` từ getContent(), getNumber(), getSize(), getTotalElements(), getTotalPages() và isLast(). totalItems dùng int theo đề, chuyển từ long bằng Math.toIntExact().

Service dùng `@Transactional(readOnly = true)`. `getPagedCourses()` của bài trước được giữ lại; API hiện tại gọi `getPagedCoursesByStatus()`. Hai phương thức dùng chung phần tạo Pageable và map DTO.

Repository kế thừa `JpaRepository<Course, Long>` và khai báo JPQL constructor expression:

```java
@Query("""
        SELECT new com.example.baitapsession05.dto.CourseResponseV2(c.id, c.title, c.status)
        FROM Course c
        WHERE c.status = :status
        """)
Page<CourseResponseV2> findAllByStatusV2(@Param("status") CourseStatus status, Pageable pageable);
```

`Course` và `status` là tên entity/thuộc tính Java. JPQL không viết LIMIT/OFFSET hay ORDER BY cố định; Spring Data áp dụng Pageable và Sort, đồng thời tính tổng số bản ghi theo điều kiện lọc.

### DTO và response

`CourseResponseV2` chỉ có `id`, `title`, `status`, có constructor đúng thứ tự tham số của JPQL. JSON không chứa Instructor, instructorId hay danh sách enrollments. CourseResponse V1 vẫn được giữ cho ví dụ mapping của bài trước.

Ví dụ rút gọn khi database có 3 khóa học Alpha, Beta, Gamma:

```json
{
  "success": true,
  "message": "Lấy danh sách khóa học thành công",
  "data": {
    "items": [
      { "id": 2, "title": "Alpha", "status": "ACTIVE" },
      { "id": 3, "title": "Beta", "status": "ACTIVE" }
    ],
    "page": 0,
    "size": 2,
    "totalItems": 3,
    "totalPages": 2,
    "last": false
  }
}
```

Response chỉ có đúng 6 trường trong data: items, page, size, totalItems, totalPages, last. Không đưa pageable, sort hay entity quan hệ vào response. ID trong ví dụ chỉ minh họa; ID thực tế phụ thuộc database.

Các API POST/PUT từ Session04 trả thông điệp với `data=null`; API GET đưa PageResponse DTO vào `data`.

## Thử bằng Postman

1. Chạy ứng dụng.
2. Import `postman/Session05-Bai1.postman_collection.json`.
3. Để collection variable `baseUrl=http://localhost:8080`.
4. Chạy collection theo thứ tự: nhóm đầu tạo một giảng viên và ba khóa học Gamma, Alpha, Beta; nhóm sau kiểm tra phân trang/sắp xếp và lỗi đầu vào.

Collection có 13 request và tự lấy ID giảng viên từ header Location. Mỗi lần chạy thêm một giảng viên và ba khóa học, không xóa dữ liệu đang có. Sau lần kiểm tra trên máy hiện tại, database đã có dữ liệu demo để gọi GET trực tiếp.

Một số URL để thử:

```text
GET /courses
GET /courses?page=0&size=2&sortBy=title&direction=ASC
GET /courses?page=1&size=2&sortBy=title&direction=ASC
GET /courses?page=0&size=2&sortBy=title&direction=ASC&status=INACTIVE
GET /courses?page=-5&size=2
GET /courses?size=0
GET /courses?sortBy=unknown
GET /courses?direction=SIDEWAYS
GET /courses?status=UNKNOWN
```

## Kiểm tra tự động

### Tái hiện N+1 và kiểm tra projection

Trong test `comparesNPlusOneWithTenInstructorsAgainstDtoProjection`, chương trình tạo 10 Course COMPLETED và 10 Instructor khác nhau, flush/clear persistence context rồi so sánh:

- Query Page<Course> và đọc `course.getInstructor().getName()`: **12 SQL** = 1 lấy Course + 1 đếm tổng + 10 tải Instructor.
- Query Page<CourseResponseV2>: **2 SQL** = 1 lấy ba cột DTO + 1 đếm tổng; không tải entity Course/Instructor.

Chạy riêng method test bằng IntelliJ để xem SQL trong Run console và dòng `N+1 demo: ...`. Dữ liệu test rollback nên không thêm các bản ghi này vào database ứng dụng.

V1 hiện tại chỉ đọc ID giảng viên. Hibernate có thể lấy ID từ lazy proxy mà không SELECT Instructor; do đó test đọc thêm tên để tái hiện đúng N+1. Projection V2 giảm dữ liệu lấy từ repository; đây là ví dụ học thuật, không thay thế các chiến lược fetch cho mọi trường hợp N+1.

```powershell
.\gradlew.bat test bootJar
```

Các test dùng profile `test`, kết nối database riêng **course_management_session05_test** và rollback dữ liệu sau mỗi test. Database này đã được tạo trên máy hiện tại. Trên máy khác, chạy `sql/create-test-database.sql` trước khi chạy test; có thể dùng biến `TEST_DB_URL` để thay đổi kết nối test.

`CoursePaginationTests` kiểm tra mặc định ACTIVE khi database có cả ACTIVE/INACTIVE; lọc INACTIVE rồi phân trang ASC/DESC, tổng số phần tử theo bộ lọc, trang rỗng và status sai. Các test có sẵn được dùng lại, không thêm file test.

Nếu gọi status=INACTIVE mà items rỗng, database chưa có khóa học INACTIVE. Có thể tạo bằng POST /courses với body có status="INACTIVE" và instructorId hợp lệ rồi gọi lại.

## Dùng bản ZIP trên máy khác

1. Cài Java 21 và PostgreSQL.
2. Trong database `postgres`, chạy `sql/create-database.sql` để tạo database ứng dụng. Nếu muốn chạy test, tạo thêm database bằng `sql/create-test-database.sql`.
3. Sao chép `config/application-local.properties.example` thành `config/application-local.properties`, điền tài khoản PostgreSQL của máy đó.
4. Mở project rồi chạy như hướng dẫn trên. Hibernate tự tạo bảng bằng `ddl-auto=update`.

Có thể cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` bằng biến môi trường.

Tài liệu tham khảo: [Spring Data Page.map](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/domain/Page.html) và [Spring Data web support](https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html).

JPQL constructor expression: [Spring Data JPA DTO Projections](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html).
