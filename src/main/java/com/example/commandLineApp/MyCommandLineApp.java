package com.example.commandLineApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class MyCommandLineApp implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MyCommandLineApp.class);

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            logger.info("Arguments passed: ");
            for (String arg : args) {
                logger.info(arg);
            }
        } else {
            logger.info("No arguments passed.");
        }

        if(args.length != 2){
            System.out.println("Program pokreni s tocno dva argumenta: geojson mora/kopna/otoka, geojson rute");
            return;
        }
        String geomDataJson = FileToString(args[0]);
        String ruteJson = FileToString(args[1]);
        Validator.validate(geomDataJson, ruteJson);
    }

    private String FileToString(String pathString) throws IOException {
        return Files.readString(Paths.get(pathString),StandardCharsets.UTF_8);
    }
}
