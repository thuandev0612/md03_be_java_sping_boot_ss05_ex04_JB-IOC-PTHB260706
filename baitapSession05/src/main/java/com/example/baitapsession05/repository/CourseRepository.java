package com.example.baitapsession05.repository;

import com.example.baitapsession05.dto.CourseResponseV2;
import com.example.baitapsession05.model.Course;
import com.example.baitapsession05.model.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE c.status = :status")
    Page<Course> findAllByStatus(@Param("status") CourseStatus status, Pageable pageable);

    @Query("""
            SELECT new com.example.baitapsession05.dto.CourseResponseV2(c.id, c.title, c.status)
            FROM Course c
            WHERE c.status = :status
            """)
    Page<CourseResponseV2> findAllByStatusV2(@Param("status") CourseStatus status, Pageable pageable);
}
