package org.example.homeandgarden.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for creating a new category")
public class CategoryRequest {

    @JsonProperty("categoryName")
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Invalid category name: Must be of 2 - 50 characters")
    @Schema(description = "Name of the category")
    private String categoryName;
}
