package com.restaurant.menu.repository;

import com.restaurant.menu.entity.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Category> rowMapper = (rs, rowNum) -> {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setSortOrder(rs.getInt("sort_order"));
        return c;
    };

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findAll() {
        return jdbcTemplate.query("SELECT * FROM categories ORDER BY sort_order", rowMapper);
    }

    public Optional<Category> findById(Long id) {
        List<Category> results = jdbcTemplate.query(
            "SELECT * FROM categories WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Category> findByName(String name) {
        List<Category> results = jdbcTemplate.query(
            "SELECT * FROM categories WHERE name = ?", rowMapper, name);
        return results.stream().findFirst();
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
    }

    public Category save(Category category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO categories (name, sort_order) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getName());
            ps.setInt(2, category.getSortOrder() != null ? category.getSortOrder() : 0);
            return ps;
        }, keyHolder);
        category.setId(keyHolder.getKey().longValue());
        return category;
    }
}
