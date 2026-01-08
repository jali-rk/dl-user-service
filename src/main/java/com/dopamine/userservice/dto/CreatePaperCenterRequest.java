package com.dopamine.userservice.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Request DTO for creating a paper center.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaperCenterRequest {
    @NotBlank(message = "Paper center name is required")
    @Size(max = 255, message = "Paper center name must not exceed 255 characters")
    private String name;
}
