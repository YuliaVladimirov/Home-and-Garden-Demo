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
@Schema(description = "Response for displaying new access token")
public class RefreshResponse {

    @JsonProperty("type")
    @Schema(description = "Type of tokens")
    private final String type = "Bearer";

    @JsonProperty("accessToken")
    @Schema(description = "New access token")
    private String accessToken;

}
