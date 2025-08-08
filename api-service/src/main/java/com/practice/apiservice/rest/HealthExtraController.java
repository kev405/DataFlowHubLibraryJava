package com.practice.apiservice.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.error.ErrorHandler;

@RestController
public class HealthExtraController {

    private static final Logger logger = LoggerFactory.getLogger(HealthExtraController.class);

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/ping-with-error-demo")
    public String pingWithErrorDemo() {
        try {
            // Example: simulate an error to demonstrate ErrorHandler usage
            throw new RuntimeException("This is a demo error");
        } catch (Exception e) {
            // Use ErrorHandler from core to format and log the error
            ErrorHandler.log(logger, e, false);
            return "Error handled - check logs";
        }
    }
}
