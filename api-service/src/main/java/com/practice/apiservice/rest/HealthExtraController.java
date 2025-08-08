package com.practice.apiservice.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.utils.error.ErrorHandler;

@RestController
@RequiredArgsConstructor
public class HealthExtraController {

    private final ErrorHandler errorHandler;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
