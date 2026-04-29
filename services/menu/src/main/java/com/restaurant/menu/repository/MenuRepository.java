package com.restaurant.menu.repository;

import com.restaurant.menu.entity.Menu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MenuRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Menu> rowMapper = (rs, rowNum) -> {
        Menu m = new Menu();
        m.setId(rs.getLong("id"));
        m.setCategoryId(rs.getLong("category_id"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setPrice(rs.getInt("price"));
        m.setImageUrl(rs.getString("image_url"));
        m.setTags(rs.getString("tags"));
        m.setAllergens(rs.getString("allergens"));
        m.setSpicyLevel(rs.getInt("spicy_level"));
        m.setCookTimeMinutes(rs.getInt("cook_time_minutes"));
        m.setIsSoldOut(rs.getInt("is_sold_out") == 1);
        m.setIsBest(rs.getInt("is_best") == 1);
        String createdAt = rs.getString("created_at");
        if (createdAt != null) m.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) m.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
        return m;
    };

    public MenuRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Menu> findAll() {
        return jdbcTemplate.query("SELECT * FROM menus ORDER BY id", rowMapper);
    }

    public Optional<Menu> findById(Long id) {
        List<Menu> results = jdbcTemplate.query(
            "SELECT * FROM menus WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    public List<Menu> findByCategoryId(Long categoryId) {
        return jdbcTemplate.query(
            "SELECT * FROM menus WHERE category_id = ? ORDER BY id", rowMapper, categoryId);
    }

    public List<Menu> findBest() {
        return jdbcTemplate.query(
            "SELECT * FROM menus WHERE is_best = 1 ORDER BY id", rowMapper);
    }

    public List<Menu> search(Long categoryId, String keyword, int offset, int limit, String orderBy) {
        StringBuilder sql = new StringBuilder("SELECT * FROM menus WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (name LIKE ? OR description LIKE ? OR tags LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY ").append(orderBy).append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public long count(Long categoryId, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM menus WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (name LIKE ? OR description LIKE ? OR tags LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    }

    public Menu save(Menu menu) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO menus (category_id, name, description, price, image_url, tags, allergens, spicy_level, cook_time_minutes, is_sold_out, is_best) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, menu.getCategoryId());
            ps.setString(2, menu.getName());
            ps.setString(3, menu.getDescription());
            ps.setInt(4, menu.getPrice());
            ps.setString(5, menu.getImageUrl());
            ps.setString(6, menu.getTags());
            ps.setString(7, menu.getAllergens());
            ps.setInt(8, menu.getSpicyLevel() != null ? menu.getSpicyLevel() : 0);
            ps.setInt(9, menu.getCookTimeMinutes() != null ? menu.getCookTimeMinutes() : 15);
            ps.setInt(10, menu.getIsSoldOut() != null && menu.getIsSoldOut() ? 1 : 0);
            ps.setInt(11, menu.getIsBest() != null && menu.getIsBest() ? 1 : 0);
            return ps;
        }, keyHolder);
        menu.setId(keyHolder.getKey().longValue());
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());
        return menu;
    }

    public void update(Menu menu) {
        String now = LocalDateTime.now().toString();
        jdbcTemplate.update(
            "UPDATE menus SET category_id = ?, name = ?, description = ?, price = ?, image_url = ?, " +
            "tags = ?, allergens = ?, spicy_level = ?, cook_time_minutes = ?, updated_at = ? " +
            "WHERE id = ?",
            menu.getCategoryId(), menu.getName(), menu.getDescription(), menu.getPrice(),
            menu.getImageUrl(), menu.getTags(), menu.getAllergens(),
            menu.getSpicyLevel(), menu.getCookTimeMinutes(), now, menu.getId()
        );
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM menus WHERE id = ?", id);
    }

    public void toggleSoldOut(Long id) {
        jdbcTemplate.update(
            "UPDATE menus SET is_sold_out = CASE WHEN is_sold_out = 0 THEN 1 ELSE 0 END, " +
            "updated_at = ? WHERE id = ?", LocalDateTime.now().toString(), id);
    }

    public void updateBestFlag(Long id, boolean isBest) {
        jdbcTemplate.update(
            "UPDATE menus SET is_best = ?, updated_at = ? WHERE id = ?",
            isBest ? 1 : 0, LocalDateTime.now().toString(), id);
    }
}
