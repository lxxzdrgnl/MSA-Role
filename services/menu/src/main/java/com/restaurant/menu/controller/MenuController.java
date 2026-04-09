package com.restaurant.menu.controller;

import com.restaurant.menu.dto.*;
import com.restaurant.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Menu", description = "메뉴/카테고리 관리 API")
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "메뉴 목록 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "메뉴 상세 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "메뉴 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/menus/1\",\"status\":404,\"code\":\"RESOURCE_NOT_FOUND\",\"message\":\"요청한 리소스를 찾을 수 없습니다.\"}")))
    })
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
    @Operation(summary = "메뉴 생성 (관리자)", description = "새 메뉴를 등록합니다. X-User-Role: ADMIN 헤더 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "헤더 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/menus\",\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"필수 헤더가 누락되었습니다: X-User-Role\"}"))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"timestamp\":\"2025-03-05T12:00:00Z\",\"path\":\"/api/menus\",\"status\":403,\"code\":\"FORBIDDEN\",\"message\":\"관리자 권한이 필요합니다\"}"))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "메뉴 삭제 (관리자)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "헤더 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "메뉴 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
