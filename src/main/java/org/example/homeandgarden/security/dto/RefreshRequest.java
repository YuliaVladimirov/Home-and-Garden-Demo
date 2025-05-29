package org.example.homeandgarden.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request for receiving a new access token")
public class RefreshRequest {

    @JsonProperty("refreshToken")
    @NotBlank(message = "Refresh token is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$", message = "Invalid refresh token format")
    @Schema(description = "Requested refresh token")
    public String refreshToken;
}