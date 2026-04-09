package com.restaurant.menu.controller;

import com.restaurant.menu.dto.*;
import com.restaurant.menu.service.MenuService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(menuService.getMenus(category, keyword, page, size, sort));
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

    /**
     * Create menu — accepts multipart/form-data with individual fields.
     * Each field sent as a separate form field (not a nested JSON part).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponse> createMenu(
            @RequestHeader("X-User-Role") String role,
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer price,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String allergens,
            @RequestParam(required = false, defaultValue = "0") Integer spicyLevel,
            @RequestParam(required = false, defaultValue = "15") Integer cookTimeMinutes,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        checkAdmin(role);
        MenuCreateRequest request = new MenuCreateRequest(
            categoryId, name, description, price, tags, allergens, spicyLevel, cookTimeMinutes
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createMenu(request, image));
    }

    /**
     * Update menu — same multipart/form-data approach.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponse> updateMenu(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer price,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String allergens,
            @RequestParam(required = false) Integer spicyLevel,
            @RequestParam(required = false) Integer cookTimeMinutes,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        checkAdmin(role);
        MenuUpdateRequest request = new MenuUpdateRequest(
            categoryId, name, description, price, tags, allergens, spicyLevel, cookTimeMinutes
        );
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

    @PatchMapping("/{id}/best-seller")
    public ResponseEntity<MenuResponse> toggleBestSeller(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        return ResponseEntity.ok(menuService.toggleBestSeller(id));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CategoryRequest request) {
        checkAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        checkAdmin(role);
        return ResponseEntity.ok(menuService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        menuService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("관리자 권한이 필요합니다");
        }
    }
}
