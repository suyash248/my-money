package org.navi.mymoney;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.navi.mymoney.dao.DataStub;
import org.navi.mymoney.models.Portfolio;
import org.navi.mymoney.services.PortfolioService;
import org.navi.mymoney.services.PortfolioServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DriverTest {
    @Mock
    private DataStub dataStub;
    @Mock
    private PortfolioService portfolioService;
    @Spy
    private Portfolio portfolio;
    @Spy
    private Driver driver;

    @BeforeEach
    public void setUp() {
        dataStub = new DataStub();
        portfolio = new Portfolio(new HashSet<>());
        portfolioService = new PortfolioServiceImpl(dataStub, portfolio);
        driver = new Driver(portfolioService);
    }

    @Test
    void testInvalidFile() {
        assertThrows(
                IOException.class,
                () -> driver.executeCommandsFromFile("invalidInputFile"),
                "Expected Allocate method to throw Exception, but it didn't.");
    }

    @Test
    void testCommands() throws IOException {
        String inputFile = "src/test/resources/input.txt";
        String outputFile = "src/test/resources/output.txt";
        List<String> output = driver.executeCommandsFromFile(inputFile);
        try (Stream<String> lines = Files.lines(Paths.get(outputFile))) {
            String expectedResult = lines.map(String::trim).collect(Collectors.joining(";"));
            String result = output.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .collect(Collectors.joining(";"));
            assertEquals(expectedResult, result);
        }
    }

}
