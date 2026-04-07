package com.restaurant.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.gateway.handler.OrderWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/internal/ws")
public class InternalWsController {

    private final OrderWebSocketHandler wsHandler;
    private final ObjectMapper objectMapper;

    public InternalWsController(OrderWebSocketHandler wsHandler, ObjectMapper objectMapper) {
        this.wsHandler = wsHandler;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(@RequestBody Map<String, Object> payload) throws IOException {
        String userId = String.valueOf(payload.get("userId"));
        String json = objectMapper.writeValueAsString(payload);

        wsHandler.notifyUser(userId, json);
        wsHandler.notifyAdmins(json);

        return ResponseEntity.ok().build();
    }
}
