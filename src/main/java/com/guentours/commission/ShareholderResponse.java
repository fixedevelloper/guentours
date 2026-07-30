package com.guentours.commission;

import com.guentours.shared.Money;

import java.math.BigDecimal;
import java.util.List;

public record ShareholderResponse(String id, String name, BigDecimal percentage, boolean active, List<Money> balance) {

    static ShareholderResponse of(Shareholder shareholder, List<Money> balance) {
        return new ShareholderResponse(
                shareholder.getId(), shareholder.getName(), shareholder.getPercentage(),
                shareholder.isActive(), balance);
    }
}
