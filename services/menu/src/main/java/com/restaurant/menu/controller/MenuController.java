package com.restaurant.menu.controller;

import com.restaurant.menu.dto.*;
import com.restaurant.menu.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<MenuResponse>> getMenus(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(menuService.getMenus(category, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenu(id));
    }

    @GetMapping("/best")
    public ResponseEntity<List<MenuResponse>> getBestMenus() {
        return ResponseEntity.ok(menuService.getBestMenus());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(menuService.getAllCategories());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponse> createMenu(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestPart("menu") MenuCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        checkAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createMenu(request, image));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuResponse> updateMenu(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @Valid @RequestPart("menu") MenuUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        checkAdmin(role);
        return ResponseEntity.ok(menuService.updateMenu(id, request, image));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/sold-out")
    public ResponseEntity<MenuResponse> toggleSoldOut(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        return ResponseEntity.ok(menuService.toggleSoldOut(id));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CategoryRequest request) {
        checkAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createCategory(request));
    }

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("관리자 권한이 필요합니다");
        }
    }
}
