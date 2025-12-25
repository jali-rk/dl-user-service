package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated student list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedStudentsResponse {

    /**
     * List of students for the current page.
     */
    private List<StudentListItem> items;

    /**
     * Total count of all students in the database.
     */
    private Long total;
}

