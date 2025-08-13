package org.example.homeandgarden.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reset password")
public class PasswordResetRequest {

    @JsonProperty("passwordResetToken")
    @NotBlank(message = "Token is required to reset password")
    @Pattern(regexp = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$", message = "Invalid reset password token format")
    @Schema(description = "Requested token to reset password")
    private String passwordResetToken;

    @JsonProperty("newPassword")
    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
            message = "Invalid value for 'new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")
    @Schema(description = "User's new password")
    private String newPassword;

    @JsonProperty("confirmNewPassword")
    @NotBlank(message = "Confirm new password field is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
            message = "Invalid value for 'confirm new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")
    @Schema(description = "Confirmation of user's new password")
    private String confirmPassword;
}

