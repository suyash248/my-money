package org.navi.mymoney.dao;

import org.navi.mymoney.constants.AssetClass;
import org.navi.mymoney.models.AssetHolding;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Month;
import java.util.*;

@Scope("singleton")
@Component
public class DataStub {
    public Map<Month, Set<AssetHolding>> monthlyBalance = new TreeMap<>();
    public Map<AssetClass, BigDecimal> initialSip = new HashMap<>();
    public Map<AssetClass, BigDecimal> desiredWeights = new HashMap<>();
    public Map<Month, Map<AssetClass, Double>> monthlyMarketChangeRate = new TreeMap<>();
}
