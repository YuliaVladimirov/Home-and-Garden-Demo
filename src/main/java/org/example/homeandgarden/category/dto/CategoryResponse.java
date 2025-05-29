package org.example.homeandgarden.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.homeandgarden.category.entity.enums.CategoryStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying a category")
public class CategoryResponse {

    @JsonProperty("categoryId")
    @Schema(description = "Unique category id (UUID)")
    private UUID categoryId;

    @JsonProperty("categoryName")
    @Schema(description = "Name of the category")
    private String categoryName;

    @JsonProperty("categoryStatus")
    @Schema(description = "Status of the category")
    private CategoryStatus categoryStatus;

    @JsonProperty("createdAt")
    @Schema(description = "Date the category was created")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Date the category was updated")
    private Instant updatedAt;
}