package org.example.homeandgarden.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for updating a user")
public class UserUpdateRequest {

    @JsonProperty("firstName")
    @Size(min = 2, max = 30, message = "Invalid first name: Must be of 2 - 30 characters")
    @Schema(description = "User's first name")
    private String firstName;

    @JsonProperty("lastName")
    @Size(min = 2, max = 30, message = "Invalid last name: Must be of 2 - 30 characters")
    @Schema(description = "User's last name")
    private String lastName;
}
