package org.example.homeandgarden.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying access and refresh tokens")
public class LoginResponse {

    @JsonProperty("type")
    @Schema(description = "Type of tokens")
    private final String type = "Bearer";

    @JsonProperty("accessToken")
    @Schema(description = "Generated access token")
    private String accessToken;

    @JsonProperty("refreshToken")
    @Schema(description = "Generated refresh token")
    private String refreshToken;
}