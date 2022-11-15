package org.navi.mymoney.services;

import org.navi.mymoney.constants.Constants;
import org.navi.mymoney.dao.DataStub;
import org.navi.mymoney.constants.AssetClass;
import org.navi.mymoney.models.AssetHolding;
import org.navi.mymoney.models.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PortfolioServiceImpl implements PortfolioService {
    private final DataStub dataStub;
    private final Portfolio portfolio;

    private final Logger logger = LoggerFactory.getLogger(PortfolioServiceImpl.class);

    public PortfolioServiceImpl(DataStub dataStub, Portfolio portfolio) {
        this.dataStub = dataStub;
        this.portfolio = portfolio;
    }

    @Override
    public void allocate(Map<AssetClass, BigDecimal> allocations) throws IllegalStateException {
        if (!portfolio.getHoldings().isEmpty()) {
            throw new IllegalStateException("The funds are already Allocated Once");
        }
        if (allocations.size() != AssetClass.values().length) {
            throw new IllegalStateException("Please allocate funds to all the asset classes.");
        }
        allocations.forEach((assetClass, amount) -> portfolio.addHolding(new AssetHolding(assetClass, amount)));

        dataStub.desiredWeights = calculateDesiredWeight();
        logger.debug("Portfolio initialized with initial allocation of {} and desired weights of {}",
                portfolio.getHoldings(), dataStub.desiredWeights);
    }

    @Override
    public void initSip(Map<AssetClass, BigDecimal> sips) throws IllegalStateException {
        // Since sip always starts from Feb, we disallow entering multiple sips
        if (!dataStub.initialSip.isEmpty()) {
            throw new IllegalStateException("The SIP is already registered once");
        }
        if (sips.size() != AssetClass.values().length) {
            throw new IllegalStateException("Please start SIP in all the asset classes.");
        }
        dataStub.initialSip.putAll(sips);
        logger.debug("Portfolio initialized with a monthly sip of {}", dataStub.initialSip);
    }

    @Override
    public void change(Map<AssetClass, Double> assetClassRates, Month month) throws InputMismatchException {
        if (Objects.nonNull(dataStub.monthlyMarketChangeRate.getOrDefault(month, null))) {
            throw new InputMismatchException(
                    "The Rate of Change for month " + month.name() + " is already registered");
        }
        if (Objects.isNull(assetClassRates) || Objects.isNull(month)) {
            throw new InputMismatchException("One of the supplied parameter is null.");
        }
        if (assetClassRates.size() != AssetClass.values().length) {
            throw new InputMismatchException("The input is not in the desired format");
        }

        dataStub.monthlyMarketChangeRate.put(month, assetClassRates);
    }

    @Override
    public String balance(Month month) {
        IntStream.range(1, month.getValue() + 1).forEach(monthNum -> {
            Month currMonth = Month.of(monthNum);
            Map<AssetClass, Double> monthlyRateChange = dataStub.monthlyMarketChangeRate.get(currMonth);
            if (!dataStub.monthlyBalance.containsKey(currMonth)) {
                portfolio.getHoldings().forEach(holding -> {
                    if (monthNum > 1) {
                        // After SIP - Starts from FEB.
                        holding.setAmountInvested(holding.getAmountInvested().add(dataStub.initialSip.get(holding.getAssetClass())));
                    }
                    // After market change
                    BigDecimal currAmount = holding.getAmountInvested();
                    BigDecimal delta = currAmount.multiply(BigDecimal.valueOf(monthlyRateChange.get(holding.getAssetClass())))
                            .divide(BigDecimal.valueOf(100), RoundingMode.FLOOR);
                    holding.setAmountInvested(currAmount.add(delta));
                });
                dataStub.monthlyBalance.put(currMonth, portfolio.getHoldings().stream().map(AssetHolding::clone).collect(Collectors.toSet()));
            }
        });

        Map<AssetClass, BigDecimal> holdings = portfolio.getHoldings().stream().collect(Collectors.toMap(AssetHolding::getAssetClass, AssetHolding::getAmountInvested));
        logger.debug("Balance after {} - {}", month, holdings);

        return Arrays.stream(AssetClass.values()).map(assetClass -> {
            BigDecimal amount = holdings.get(assetClass);
            return String.valueOf(Double.valueOf(Math.floor(amount.doubleValue())).intValue());
        }).collect(Collectors.joining(" "));
    }

    @Override
    public String reBalance() {
        if (dataStub.monthlyMarketChangeRate.size() < 6) {
            return Constants.CANNOT_REBALANCE;
        }

        Month reBalanceMonth;
        if (dataStub.monthlyMarketChangeRate.size() < 12) {
            reBalanceMonth = Month.JUNE;

            IntStream.range(Month.JUNE.getValue(), Month.DECEMBER.getValue() + 1)
                    .forEach(monthNum -> dataStub.monthlyBalance.remove(Month.of(monthNum)));

            Map<AssetClass, BigDecimal> assetAmount = dataStub.monthlyBalance.get(Month.MAY).stream().collect(
                    Collectors.toMap(AssetHolding::getAssetClass, AssetHolding::getAmountInvested));
            portfolio.getHoldings().forEach(holding -> holding.setAmountInvested(assetAmount.get(holding.getAssetClass())));

        } else {
            reBalanceMonth = Month.DECEMBER;
        }

        this.balance(reBalanceMonth);
        BigDecimal totalValue = portfolio.getTotalInvestment();
        portfolio.getHoldings().forEach(holding -> {
            BigDecimal weight = dataStub.desiredWeights.get(holding.getAssetClass());
            holding.setAmountInvested(totalValue.multiply(weight).divide(BigDecimal.valueOf(100), RoundingMode.FLOOR));
        });

        Map<AssetClass, BigDecimal> holdings = portfolio.getHoldings().stream().collect(
                Collectors.toMap(AssetHolding::getAssetClass, AssetHolding::getAmountInvested));
        logger.debug("After re-balance in {} - {}", reBalanceMonth, holdings);

        return Arrays.stream(AssetClass.values()).map(assetClass -> {
            BigDecimal amount = holdings.get(assetClass);
            return String.valueOf(Double.valueOf(Math.floor(amount.doubleValue())).intValue());
        }).collect(Collectors.joining(" "));
    }

    private Map<AssetClass, BigDecimal> calculateDesiredWeight() {
        if (portfolio.getHoldings().isEmpty()) {
            throw new IllegalStateException("No holdings found in portfolio.");
        }
        return portfolio.getHoldings().stream()
                .collect(Collectors.toMap(
                        AssetHolding::getAssetClass,
                        e -> e.getAmountInvested().multiply(BigDecimal.valueOf(100L))
                                .divide(portfolio.getTotalInvestment(), RoundingMode.FLOOR)
                ));
    }
}
