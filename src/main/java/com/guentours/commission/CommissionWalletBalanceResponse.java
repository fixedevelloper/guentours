package com.guentours.commission;

import com.guentours.shared.Money;

import java.util.List;

public record CommissionWalletBalanceResponse(List<Money> balances, long entryCount) {
}
