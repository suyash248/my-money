package org.navi.mymoney.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@AllArgsConstructor
@Getter
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Component
public class Portfolio {
    private Set<AssetHolding> holdings;
    public BigDecimal getTotalInvestment() {
        return holdings.stream().map(AssetHolding::getAmountInvested).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    public void addHolding(AssetHolding holding) {
        this.holdings.add(holding);
    }
}
