package org.navi.mymoney;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.navi.mymoney.constants.AssetClass;
import org.navi.mymoney.constants.Constants;
import org.navi.mymoney.dao.DataStub;
import org.navi.mymoney.models.Portfolio;
import org.navi.mymoney.services.PortfolioService;
import org.navi.mymoney.services.PortfolioServiceImpl;

import java.math.BigDecimal;

import static java.time.Month.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Mock
    private DataStub dataStub;
    @Mock
    private PortfolioService portfolioService;
    @Mock
    private Portfolio portfolio;

    private Map<AssetClass, BigDecimal> dummyAllocation;
    private Map<AssetClass, BigDecimal> dummySips;

    @BeforeEach
    public void setUp() {
        dataStub = new DataStub();
        portfolio = new Portfolio(new HashSet<>());
        portfolioService = new PortfolioServiceImpl(dataStub, portfolio);
//        driver = new Driver(portfolioService);
        dummyAllocation = new HashMap<>();
        dummyAllocation.put(AssetClass.EQUITY, BigDecimal.valueOf(50d));
        dummyAllocation.put(AssetClass.DEBT, BigDecimal.valueOf(30d));
        dummyAllocation.put(AssetClass.GOLD, BigDecimal.valueOf(20d));

        dummySips = new HashMap<>();
        dummySips.put(AssetClass.EQUITY, BigDecimal.valueOf(50d));
        dummySips.put(AssetClass.DEBT, BigDecimal.valueOf(30d));
        dummySips.put(AssetClass.GOLD, BigDecimal.valueOf(20d));
    }

    @Test
    void testReAllocate() throws IllegalStateException {
        portfolioService.allocate(dummyAllocation);
        assertThrows(IllegalStateException.class,
                () -> portfolioService.allocate(dummyAllocation),
                "allocate() should have thrown the exception");
    }

    @Test
    void testAllocate() throws IllegalStateException {
        Map<AssetClass, BigDecimal> dummyAllocation = new HashMap<>();
        dummyAllocation.put(AssetClass.EQUITY, BigDecimal.valueOf(50d));
        dummyAllocation.put(AssetClass.DEBT, BigDecimal.valueOf(30d));
        dummyAllocation.put(AssetClass.GOLD, BigDecimal.valueOf(20d));

        portfolioService.allocate(dummyAllocation);
        assertEquals(
                BigDecimal.valueOf(100.0), dataStub.desiredWeights.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO));
    }

    @Test
    void testAllocateInCorrectValues() throws IllegalStateException {
        Map<AssetClass, BigDecimal> invalidAllocations = new HashMap<>();
        invalidAllocations.put(AssetClass.DEBT, BigDecimal.ZERO);
        assertThrows(IllegalStateException.class,
                () -> portfolioService.allocate(invalidAllocations),
                "allocate() should have thrown the exception");
    }

    @Test
    void testAllocateAlreadyAllocated() throws IllegalStateException {
        portfolioService.allocate(dummyAllocation);

        assertThrows(IllegalStateException.class,
                () -> portfolioService.allocate(dummyAllocation),
                "allocate() should have thrown the exception");
    }

    @Test
    void testSipWithInCorrectValues() throws IllegalStateException {
        Map<AssetClass, BigDecimal> incorrectSips = new HashMap<>();
        incorrectSips.put(AssetClass.GOLD, BigDecimal.ONE);
        assertThrows(
                IllegalStateException.class,
                () -> portfolioService.initSip(incorrectSips),
                "initSip() should have thrown an exception");
    }

    @Test
    void testSip() {
        assertDoesNotThrow(
                () -> portfolioService.initSip(dummySips),
                "initSip() shouldn't have thrown exception."
        );
    }

    @Test
    void testSipAlreadyAllocated() throws IllegalStateException {
        portfolioService.initSip(dummySips);
        assertThrows(
                IllegalStateException.class,
                () -> portfolioService.initSip(dummySips),
                "initSip() should have thrown an exception.");
    }

    @Test
    void testChangeWithNullValues() throws InputMismatchException {
        assertThrows(
                InputMismatchException.class,
                () -> portfolioService.change(null, JANUARY),
                "change() should have thrown an exception.");
    }

    @Test
    void testChangeWithInCorrectValues() throws InputMismatchException {
        Map<AssetClass, Double> rates = new HashMap<>();
        assertThrows(
                InputMismatchException.class,
                () -> portfolioService.change(rates, JANUARY),
                "change() should have thrown an exception.");
    }

    @Test
    void testChange() {
        Map<AssetClass, Double> rates = new HashMap<>();
        rates.put(AssetClass.EQUITY, 10d);
        rates.put(AssetClass.DEBT, 5d);
        rates.put(AssetClass.GOLD, 7d);
        portfolioService.change(rates, MARCH);
        assertEquals(rates.size(), dataStub.monthlyMarketChangeRate.get(MARCH).size());
    }

    @Test
    void testChangeAlreadyAllocatedForMonth() throws InputMismatchException {
        Map<AssetClass, Double> rates = new HashMap<>();
        rates.put(AssetClass.EQUITY, 10d);
        rates.put(AssetClass.DEBT, 5d);
        rates.put(AssetClass.GOLD, 7d);
        portfolioService.change(rates, FEBRUARY);
        assertThrows(
                InputMismatchException.class,
                () -> portfolioService.change(rates, FEBRUARY),
                "change() should have thrown an exception.");
    }

    @Test
    void testBalance() {
        initializePortfolio();
        assertEquals("10593 7897 2272", portfolioService.balance(MARCH));
    }

    @Test
    void testReBalance() {
        initializePortfolio();
        portfolioService.balance(JUNE);
        assertEquals("23622 11811 3937", portfolioService.reBalance());
    }

    @Test
    void testReBalanceWithInsufficientData() {
        portfolioService.allocate(dummyAllocation);
        portfolioService.initSip(dummySips);
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, 4d);
            put(AssetClass.DEBT, 10d);
            put(AssetClass.GOLD, 2d);
        }}, JANUARY);
        String result = portfolioService.reBalance();
        assertEquals(Constants.CANNOT_REBALANCE, result);
    }

    private void initializePortfolio() {
        portfolioService.allocate(new HashMap<AssetClass, BigDecimal>() {{
            put(AssetClass.EQUITY, BigDecimal.valueOf(6000.0d));
            put(AssetClass.DEBT, BigDecimal.valueOf(3000.0d));
            put(AssetClass.GOLD, BigDecimal.valueOf(1000.0d));
        }});
        portfolioService.initSip(new HashMap<AssetClass, BigDecimal>() {{
            put(AssetClass.EQUITY, BigDecimal.valueOf(2000.0d));
            put(AssetClass.DEBT, BigDecimal.valueOf(1000.0d));
            put(AssetClass.GOLD, BigDecimal.valueOf(500.0d));
        }});
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, 4d);
            put(AssetClass.DEBT, 10d);
            put(AssetClass.GOLD, 2d);
        }}, JANUARY);
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, -10.00d);
            put(AssetClass.DEBT, 40.00d);
            put(AssetClass.GOLD, 0.00d);
        }}, FEBRUARY);
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, 12.50);
            put(AssetClass.DEBT, 12.50d);
            put(AssetClass.GOLD, 12.50d);
        }}, MARCH);
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, 8.00d);
            put(AssetClass.DEBT, -3.00d);
            put(AssetClass.GOLD, 7.00d);
        }}, APRIL);
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, 13.00d);
            put(AssetClass.DEBT, 21.00d);
            put(AssetClass.GOLD, 10.50d);
        }}, MAY);
        portfolioService.change(new HashMap<AssetClass, Double>() {{
            put(AssetClass.EQUITY, 10.00d);
            put(AssetClass.DEBT, 8.00d);
            put(AssetClass.GOLD, -5.00d);
        }}, JUNE);
    }
}