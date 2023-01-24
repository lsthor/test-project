package org.example;

import org.junit.jupiter.api.Test;

public class AppTest {
    @Test
    public void testAppCreate() {
        Config config = new Config();
        App app = new App(config);
        // assert http call
    }
}
