package de.stphngrtz.spring.cloud.greetings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    @Value("${who:World}")
    private String who;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping(value = "/")
    public String index() {
        log.info("GET /");
        return "Greetings-Service";
    }

    @RequestMapping(value = "/greeting")
    public String greeting() {
        log.info("GET /greeting");
        return "Hello " + who;
    }
}
