package com.example.baitapsession05.dto;

import com.example.baitapsession05.model.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseResponseV2 {

    private final Long id;
    private final String title;
    private final CourseStatus status;
}
