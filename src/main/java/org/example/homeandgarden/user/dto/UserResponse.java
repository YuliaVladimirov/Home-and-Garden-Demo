package org.example.homeandgarden.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.homeandgarden.user.entity.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying a user")
public class UserResponse {

    @JsonProperty("userId")
    @Schema(description = "Unique user id (UUID)")
    private UUID userId;

    @JsonProperty("email")
    @Schema(description = "User's email")
    private String email;

    @JsonProperty("firstName")
    @Schema(description = "User's first name")
    private String firstName;

    @JsonProperty("lastName")
    @Schema(description = "User's last name")
    private String lastName;

    @JsonProperty("userRole")
    @Schema(description = "User's role in the system")
    private UserRole userRole;

    @JsonProperty("isEnabled")
    @Schema(description = "User's 'enabled' status in the system")
    private Boolean isEnabled = true;

    @JsonProperty("isNonLocked")
    @Schema(description = "User's 'non-locked' status in the system")
    private Boolean isNonLocked = true;

    @JsonProperty("registeredAt")
    @Schema(description = "Date the user was registered")
    private Instant registeredAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Date the user was last updated")
    private Instant updatedAt;

}