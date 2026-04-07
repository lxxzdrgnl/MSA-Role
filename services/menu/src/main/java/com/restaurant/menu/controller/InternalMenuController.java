package com.restaurant.menu.controller;

import com.restaurant.menu.dto.MenuResponse;
import com.restaurant.menu.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/menus")
public class InternalMenuController {

    private final MenuService menuService;

    public InternalMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<List<MenuResponse>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenus());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenu(id));
    }

    @PatchMapping("/{id}/best")
    public ResponseEntity<Void> updateBestFlag(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean isBest = body.getOrDefault("isBest", false);
        menuService.updateBestFlag(id, isBest);
        return ResponseEntity.ok().build();
    }
}
