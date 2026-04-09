package com.restaurant.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private static final String VERSION = "1.0.0";
    private static final String BUILD_TIME = Instant.now().toString();

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", VERSION,
                "buildTime", BUILD_TIME
        ));
    }
}
