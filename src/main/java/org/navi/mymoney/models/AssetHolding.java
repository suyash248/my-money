package org.navi.mymoney.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.navi.mymoney.constants.AssetClass;

import java.math.BigDecimal;
import java.util.Objects;

@AllArgsConstructor
@Getter
@ToString
public class AssetHolding implements Cloneable {
    private AssetClass assetClass;

    @Setter
    private BigDecimal amountInvested;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetHolding holding = (AssetHolding) o;
        return assetClass == holding.assetClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetClass);
    }

    @Override
    public AssetHolding clone() {
        return new AssetHolding(this.assetClass, this.amountInvested);
    }
}
