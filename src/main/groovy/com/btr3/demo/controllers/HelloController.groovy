package com.btr3.demo.controllers

import com.btr3.demo.model.Greeting
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.atomic.AtomicLong

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class)
    private static final String template = "Hello, %s, you fantastic %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!"
    }

    @RequestMapping("/greet/any")
    public Greeting sayHello(@AuthenticationPrincipal UserDetails customUser) {
        logger.info("Replying with a generic greeting")
        return new Greeting(counter.incrementAndGet(), String.format(template, customUser.getUsername(), "employee"));
    }

    @Secured("ROLE_DEVELOPERS")
    @RequestMapping("/greet/dev")
    public Greeting sayHelloDev(@AuthenticationPrincipal UserDetails customUser) {
        logger.info("Replying with a developer-specific greeting")
        return new Greeting(counter.incrementAndGet(), String.format(template, customUser.getUsername(), "developer"));
    }


}
