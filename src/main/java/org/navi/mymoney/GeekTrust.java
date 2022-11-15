package org.navi.mymoney;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.InputMismatchException;
import java.util.List;

@SpringBootApplication
public class GeekTrust implements CommandLineRunner {

    final Driver driver;

    public GeekTrust(Driver driver) {
        this.driver = driver;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GeekTrust.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.out.println("No input arguments were supplied");
            throw new InputMismatchException(
                    "Please specify input file, or to run in CLI mode provide SHELL as argument");
        } else if (args.length != 1) {
            System.out.println("No input arguments were supplied");
            throw new InputMismatchException(
                    "Please specify only the input file, or to run in CLI mode provide SHELL as argument");
        }
        String input = args[0];
        if ("shell".equalsIgnoreCase(input)) {
            System.out.println("Switching to SHELL Mode");
            return;
        }
        System.out.println("Switching to BATCH-PROCESSING Mode");
        List<String> results = driver.executeCommandsFromFile(input);
        System.exit(0);
    }
}
