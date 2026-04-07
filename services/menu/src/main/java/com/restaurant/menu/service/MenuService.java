package com.restaurant.menu.service;

import com.restaurant.menu.dto.*;
import com.restaurant.menu.entity.Category;
import com.restaurant.menu.entity.Menu;
import com.restaurant.menu.repository.CategoryRepository;
import com.restaurant.menu.repository.MenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);
    private static final String IMAGE_DIR = "/data/images/";

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.recommendation.url}")
    private String aiRecommendationUrl;

    public MenuService(MenuRepository menuRepository, CategoryRepository categoryRepository) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.restTemplate = new RestTemplate();
    }

    // --- Category ---

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(CategoryResponse::from)
            .toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + request.name());
        }
        Category category = new Category();
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    // --- Menu CRUD ---

    public PageResponse<MenuResponse> getMenus(Long categoryId, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Menu> menus = menuRepository.search(categoryId, keyword, offset, size);
        long totalCount = menuRepository.count(categoryId, keyword);
        List<MenuResponse> content = menus.stream()
            .map(this::toResponse)
            .toList();
        return PageResponse.of(content, page, size, totalCount);
    }

    public MenuResponse getMenu(Long id) {
        Menu menu = menuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + id));
        return toResponse(menu);
    }

    public List<MenuResponse> getBestMenus() {
        return menuRepository.findBest().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public MenuResponse createMenu(MenuCreateRequest request, MultipartFile image) {
        categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.categoryId()));

        Menu menu = new Menu();
        menu.setCategoryId(request.categoryId());
        menu.setName(request.name());
        menu.setDescription(request.description());
        menu.setPrice(request.price());
        menu.setTags(request.tags());
        menu.setAllergens(request.allergens());
        menu.setSpicyLevel(request.spicyLevel() != null ? request.spicyLevel() : 0);
        menu.setCookTimeMinutes(request.cookTimeMinutes() != null ? request.cookTimeMinutes() : 15);
        menu.setIsSoldOut(false);
        menu.setIsBest(false);

        if (image != null && !image.isEmpty()) {
            menu.setImageUrl(saveImage(image));
        }

        Menu saved = menuRepository.save(menu);
        syncEmbedding(saved);
        return toResponse(saved);
    }

    public MenuResponse updateMenu(Long id, MenuUpdateRequest request, MultipartFile image) {
        Menu menu = menuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + id));

        if (request.categoryId() != null) {
            categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.categoryId()));
            menu.setCategoryId(request.categoryId());
        }
        if (request.name() != null) menu.setName(request.name());
        if (request.description() != null) menu.setDescription(request.description());
        if (request.price() != null) menu.setPrice(request.price());
        if (request.tags() != null) menu.setTags(request.tags());
        if (request.allergens() != null) menu.setAllergens(request.allergens());
        if (request.spicyLevel() != null) menu.setSpicyLevel(request.spicyLevel());
        if (request.cookTimeMinutes() != null) menu.setCookTimeMinutes(request.cookTimeMinutes());

        if (image != null && !image.isEmpty()) {
            menu.setImageUrl(saveImage(image));
        }

        menuRepository.update(menu);
        Menu updated = menuRepository.findById(id).orElseThrow();
        syncEmbedding(updated);
        return toResponse(updated);
    }

    public void deleteMenu(Long id) {
        menuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + id));
        menuRepository.deleteById(id);
        deleteEmbedding(id);
    }

    public MenuResponse toggleSoldOut(Long id) {
        menuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + id));
        menuRepository.toggleSoldOut(id);
        return toResponse(menuRepository.findById(id).orElseThrow());
    }

    public void updateBestFlag(Long id, boolean isBest) {
        menuRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + id));
        menuRepository.updateBestFlag(id, isBest);
    }

    // --- Image ---

    private String saveImage(MultipartFile file) {
        try {
            String uuid = UUID.randomUUID().toString();
            String filename = uuid + "_" + file.getOriginalFilename();
            Path dir = Paths.get(IMAGE_DIR);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            return "/images/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패", e);
        }
    }

    // --- AI Embedding sync (fire-and-forget) ---

    private void syncEmbedding(Menu menu) {
        try {
            String url = aiRecommendationUrl + "/embeddings/sync";
            restTemplate.postForEntity(url, Map.of(
                "menu_id", menu.getId(),
                "name", menu.getName(),
                "description", menu.getDescription() != null ? menu.getDescription() : "",
                "price", menu.getPrice(),
                "tags", menu.getTags() != null ? menu.getTags() : "",
                "allergens", menu.getAllergens() != null ? menu.getAllergens() : "",
                "spicy_level", menu.getSpicyLevel(),
                "cook_time_minutes", menu.getCookTimeMinutes()
            ), Void.class);
        } catch (Exception e) {
            log.warn("AI 임베딩 동기화 실패 (menu_id={}): {}", menu.getId(), e.getMessage());
        }
    }

    private void deleteEmbedding(Long menuId) {
        try {
            String url = aiRecommendationUrl + "/embeddings/" + menuId;
            restTemplate.delete(url);
        } catch (Exception e) {
            log.warn("AI 임베딩 삭제 실패 (menu_id={}): {}", menuId, e.getMessage());
        }
    }

    // --- Helper ---

    private MenuResponse toResponse(Menu menu) {
        String categoryName = categoryRepository.findById(menu.getCategoryId())
            .map(Category::getName)
            .orElse("Unknown");
        return MenuResponse.from(menu, categoryName);
    }
}
