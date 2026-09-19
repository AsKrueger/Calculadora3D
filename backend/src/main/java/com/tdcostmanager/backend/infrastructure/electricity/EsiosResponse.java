package com.tdcostmanager.backend.infrastructure.electricity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record EsiosResponse(
    @JsonProperty("indicator") IndicatorData indicator
) {
    public record IndicatorData(
        @JsonProperty("name") String name,
        @JsonProperty("values") List<PriceValue> values
    ) {}

    public record PriceValue(
        @JsonProperty("value") BigDecimal value,
        @JsonProperty("datetime") String datetime,
        @JsonProperty("geo_id") Integer geoId
    ) {}
}
