package com.restaurant.review.repository;

import com.restaurant.review.entity.Review;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Review> ROW_MAPPER = (rs, rowNum) -> {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setUserId(rs.getLong("user_id"));
        review.setOrderId(rs.getLong("order_id"));
        review.setMenuId(rs.getLong("menu_id"));
        review.setMenuName(rs.getString("menu_name"));
        review.setRating(rs.getInt("rating"));
        review.setContent(rs.getString("content"));
        review.setIsAiGenerated(rs.getInt("is_ai_generated"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            review.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }
        return review;
    };

    public ReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Review save(Review review) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO reviews (user_id, order_id, menu_id, menu_name, rating, content, is_ai_generated) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, review.getUserId());
            ps.setLong(2, review.getOrderId());
            ps.setLong(3, review.getMenuId());
            ps.setString(4, review.getMenuName());
            ps.setInt(5, review.getRating());
            ps.setString(6, review.getContent());
            ps.setInt(7, review.getIsAiGenerated() != null ? review.getIsAiGenerated() : 0);
            return ps;
        }, keyHolder);
        review.setId(keyHolder.getKey().longValue());
        return review;
    }

    public Optional<Review> findById(Long id) {
        List<Review> results = jdbcTemplate.query(
                "SELECT * FROM reviews WHERE id = ?", ROW_MAPPER, id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Review> findAll(int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews ORDER BY created_at DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviews", Long.class);
        return count != null ? count : 0;
    }

    public List<Review> findByMenuId(Long menuId, int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews WHERE menu_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, menuId, limit, offset
        );
    }

    public long countByMenuId(Long menuId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE menu_id = ?", Long.class, menuId
        );
        return count != null ? count : 0;
    }

    public List<Review> findByOrderId(Long orderId, int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews WHERE order_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, orderId, limit, offset
        );
    }

    public long countByOrderId(Long orderId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE order_id = ?", Long.class, orderId
        );
        return count != null ? count : 0;
    }

    public List<Review> findByUserId(Long userId, int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, userId, limit, offset
        );
    }

    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE user_id = ?", Long.class, userId
        );
        return count != null ? count : 0;
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", id);
    }

    public Double getAverageRating() {
        Double avg = jdbcTemplate.queryForObject("SELECT AVG(CAST(rating AS REAL)) FROM reviews", Double.class);
        return avg != null ? avg : 0.0;
    }

    public long getTotalCount() {
        return countAll();
    }

    public List<Map<String, Object>> getRatingDistribution() {
        return jdbcTemplate.queryForList(
                "SELECT rating, COUNT(*) as count FROM reviews GROUP BY rating ORDER BY rating"
        );
    }

    public Double getAverageRatingByMenuId(Long menuId) {
        Double avg = jdbcTemplate.queryForObject(
                "SELECT AVG(CAST(rating AS REAL)) FROM reviews WHERE menu_id = ?", Double.class, menuId
        );
        return avg != null ? avg : 0.0;
    }

    public long getTotalCountByMenuId(Long menuId) {
        return countByMenuId(menuId);
    }

    public List<Map<String, Object>> getRatingDistributionByMenuId(Long menuId) {
        return jdbcTemplate.queryForList(
                "SELECT rating, COUNT(*) as count FROM reviews WHERE menu_id = ? GROUP BY rating ORDER BY rating",
                menuId
        );
    }

    public List<Review> findRecentByMenuId(Long menuId, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews WHERE menu_id = ? ORDER BY created_at DESC LIMIT ?",
                ROW_MAPPER, menuId, limit
        );
    }

    public List<Review> findRecent(int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews ORDER BY created_at DESC LIMIT ?",
                ROW_MAPPER, limit
        );
    }
}
