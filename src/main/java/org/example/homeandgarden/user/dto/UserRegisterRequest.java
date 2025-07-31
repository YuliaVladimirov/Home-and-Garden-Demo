package org.example.homeandgarden.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to register a new user")
public class UserRegisterRequest {

    @JsonProperty("email")
    @NotBlank(message = "Email is required")
    @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Invalid email")
    @Schema(description = "User's email")
    private String email;

    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
            message = "Invalid value for password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")
    @Schema(description = "User's password")
    private String password;

    @JsonProperty("confirmPassword")
    @NotBlank(message = "Confirm password field is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
            message = "Invalid value for confirm password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")
    @Schema(description = "Confirmation of user's password")
    private String confirmPassword;

    @JsonProperty("firstName")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 30, message = "Invalid first name: Must be of 2 - 30 characters")
    @Schema(description = "User's first name")
    private String firstName;

    @JsonProperty("lastName")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 30, message = "Invalid last name: Must be of 2 - 30 characters")
    @Schema(description = "User's last name")
    private String lastName;
}


