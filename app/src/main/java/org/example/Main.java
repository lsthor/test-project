package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.utils.ConfigParser;

import java.io.IOException;

@Slf4j
public class Main {
    public static void main(String[] args) throws IOException {
        Config config = ConfigParser.parse(args);
        new App(config);
    }
}
