package org.example;

import express.Express;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.regex.Pattern;

@Slf4j
public class Service {
    private final Express app;

    private static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]{4,5}");
        return pattern.matcher(str).matches();
    }

    public Service() {
        this(System.getenv("SERVICE_PORT") != null && isNumeric(System.getenv("SERVICE_PORT")) ? Integer.parseInt(System.getenv("SERVICE_PORT")) : 8000);
    }

    @Builder
    public Service(int port) {
        app = new Express();
        log.info("Server started at port {}", port);
        app.post("/", (req, res) -> {
            log.info("post /");
            Scanner sc = new Scanner(req.getBody());
            //Reading line by line from scanner to StringBuffer
            StringBuffer sb = new StringBuffer();
            while (sc.hasNext()) {
                sb.append(sc.nextLine());
            }
            log.info("incoming {}", sb);
            res.send(sb.toString());
        });
        app.get("/", ((req, res) -> {
            log.info("get /");
            res.send("ok");
        }));
        app.listen(port);
    }

    public void stop() {
        app.stop();
    }
}