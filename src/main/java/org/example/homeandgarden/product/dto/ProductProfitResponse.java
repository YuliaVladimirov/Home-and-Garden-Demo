package org.example.homeandgarden.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for displaying aggregation queries")
public class ProductProfitResponse {

    @JsonProperty("timeUnit")
    @Schema(description = "Units of time (days, weeks, months, years)")
    private String timeUnit;

    @JsonProperty("timePeriod")
    @Schema(description = "Amount of time for the query")
    private Integer timePeriod;

    @JsonProperty("profit")
    @Schema(description = "Profit for a certain period")
    private BigDecimal profit;
}
