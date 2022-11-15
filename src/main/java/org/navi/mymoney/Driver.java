package org.navi.mymoney;

import org.apache.commons.lang3.StringUtils;
import org.navi.mymoney.constants.AssetClass;
import org.navi.mymoney.constants.Command;
import org.navi.mymoney.services.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
public class Driver {
    private final PortfolioService portfolioService;

    private final Logger logger = LoggerFactory.getLogger(Driver.class);

    public Driver(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * Processes the command line-by-line
     *
     * @param fileName Absolute path to the input file.
     * @return <code>List<String></String></code> having the output of each command.
     * @throws IOException if command can't be parsed.
     */
    public List<String> executeCommandsFromFile(String fileName) throws IOException {
        try (Stream<String> inputLines = Files.lines(Paths.get(fileName))) {
            List<String> outputs = inputLines.filter(StringUtils::isNotEmpty)
                    .map(this::processCommand)
                    .collect(Collectors.toList());
            log(outputs);
            return outputs;
        } catch (IOException e) {
            logger.error("Invalid input file.", e);
            throw new IOException("Invalid input file");
        }
    }

    public String processCommand(String line) {
        String output = null;
        String[] commandAndInputs = line.split(" ");
        AssetClass[] assetClasses = AssetClass.values();
        int totalAssetClasses = assetClasses.length;
        try {
            Command command = Command.valueOf(commandAndInputs[0]);
            switch (command) {
                case ALLOCATE:
                    validateInput(commandAndInputs, totalAssetClasses);
                    List<BigDecimal> allocationAmounts = inputsToBigDecimals(1, totalAssetClasses, commandAndInputs);
                    Map<AssetClass, BigDecimal> allocations = new HashMap<>();
                    IntStream.range(0, totalAssetClasses).forEach(i ->
                            allocations.put(assetClasses[i], allocationAmounts.get(i)));
                    portfolioService.allocate(allocations);
                    break;
                case SIP:
                    validateInput(commandAndInputs, totalAssetClasses);
                    List<BigDecimal> sipAmounts = inputsToBigDecimals(1, totalAssetClasses, commandAndInputs);
                    Map<AssetClass, BigDecimal> sips = new HashMap<>();
                    IntStream.range(0, totalAssetClasses).forEach(i -> sips.put(assetClasses[i], sipAmounts.get(i)));
                    portfolioService.initSip(sips);
                    break;
                case CHANGE:
                    validateInput(commandAndInputs, totalAssetClasses + 1);
                    List<Double> rates =
                            Arrays.stream(commandAndInputs)
                                    .skip(1)
                                    .limit(totalAssetClasses)
                                    .map(str -> Double.parseDouble(str.replace("%", "")))
                                    .collect(Collectors.toList());

                    Map<AssetClass, Double> assetClassRates = new HashMap<>();
                    IntStream.range(0, totalAssetClasses).forEach(i ->
                            assetClassRates.put(assetClasses[i], rates.get(i)));
                    Month month = Month.valueOf(commandAndInputs[totalAssetClasses + 1]);
                    portfolioService.change(assetClassRates, month);
                    break;
                case BALANCE:
                    validateInput(commandAndInputs, 1);
                    month = Month.valueOf(commandAndInputs[1]);
                    output = portfolioService.balance(month);
                    break;
                case REBALANCE:
                    output = portfolioService.reBalance();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Command " + command + " supplied");
            }
        } catch (Exception e) {
            logger.error("Error Occurred while processing " + String.join(" ", commandAndInputs) + e.getMessage(), e);
        }
        return output;
    }

    private void validateInput(String[] commandAndInputs, int size) {
        if (commandAndInputs.length != size + 1) {
            throw new InputMismatchException("Please check the command " + String.join(" ", commandAndInputs));
        }
    }

    private List<BigDecimal> inputsToBigDecimals(int skip, int limit, String[] commandAndInputs) {
        return Arrays.stream(commandAndInputs)
                .skip(skip)
                .limit(limit)
                .map(BigDecimal::new)
                .collect(Collectors.toList());
    }

    private static void log(List<String> outputs) {
        outputs.stream().filter(Objects::nonNull).forEach(System.out::println);
    }

}
