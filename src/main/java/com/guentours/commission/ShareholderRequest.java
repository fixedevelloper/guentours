package com.guentours.commission;

import java.math.BigDecimal;

/** Create/update payload; {@code null} fields on an update leave that attribute unchanged. */
public record ShareholderRequest(String name, BigDecimal percentage, Boolean active) {
}
