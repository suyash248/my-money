package org.navi.mymoney.services;

import org.navi.mymoney.constants.AssetClass;

import java.math.BigDecimal;
import java.time.Month;
import java.util.InputMismatchException;
import java.util.Map;

public interface PortfolioService {
    void allocate(Map<AssetClass, BigDecimal> allocations) throws IllegalStateException;

    void initSip(Map<AssetClass, BigDecimal> sips) throws IllegalStateException;

    void change(Map<AssetClass, Double> rates, Month month) throws InputMismatchException;

    String balance(Month month);

    String reBalance();

//    int getSupportedAssetClass();
}
