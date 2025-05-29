package org.example.homeandgarden.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying an error")
public class ErrorResponse {

    @JsonProperty("error")
    @Schema(description = "Short error description")
    private String error;

    @JsonProperty("details")
    @Schema(description = "Detailed error message")
    private Object details;

    @JsonProperty("path")
    @Schema(description = "Url path")
    private String path;

    @JsonProperty("timestamp")
    @Schema(description = "Error timestamp")
    private Instant timestamp;

    public ErrorResponse(String error, Object details, String path) {
        this.error = error;
        this.details = details;
        this.path = path;
        this.timestamp = Instant.now();
    }
}
