package com.restaurant.order.repository;

import com.restaurant.order.entity.Order;
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
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setUserId(rs.getLong("user_id"));
        order.setStatus(rs.getString("status"));
        order.setTotalPrice(rs.getInt("total_price"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            order.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }
        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) {
            order.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
        }
        return order;
    };

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Order save(Order order) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO orders (user_id, status, total_price, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, order.getUserId());
            ps.setString(2, order.getStatus());
            ps.setInt(3, order.getTotalPrice());
            String now = java.time.LocalDateTime.now().toString();
            ps.setString(4, now);
            ps.setString(5, now);
            return ps;
        }, keyHolder);
        order.setId(keyHolder.getKey().longValue());
        return order;
    }

    public Optional<Order> findById(Long id) {
        List<Order> results = jdbcTemplate.query(
                "SELECT * FROM orders WHERE id = ?", ROW_MAPPER, id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Order> findByUserId(Long userId, int offset, int limit, String orderBy) {
        return jdbcTemplate.query(
                "SELECT * FROM orders WHERE user_id = ? ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                ROW_MAPPER, userId, limit, offset
        );
    }

    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", Long.class, userId
        );
        return count != null ? count : 0;
    }

    public List<Order> findAll(int offset, int limit, String status, String dateFrom, String dateTo, String orderBy) {
        StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            sql.append(" AND created_at >= ?");
            params.add(dateFrom + " 00:00:00");
        }
        if (dateTo != null && !dateTo.isBlank()) {
            sql.append(" AND created_at <= ?");
            params.add(dateTo + " 23:59:59");
        }
        sql.append(" ORDER BY ").append(orderBy).append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public long countAll(String status, String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM orders WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            sql.append(" AND created_at >= ?");
            params.add(dateFrom + " 00:00:00");
        }
        if (dateTo != null && !dateTo.isBlank()) {
            sql.append(" AND created_at <= ?");
            params.add(dateTo + " 23:59:59");
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public long countByStatusTotal(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = ?", Long.class, status
        );
        return count != null ? count : 0;
    }

    public List<Order> findActiveOrders() {
        return jdbcTemplate.query(
                "SELECT * FROM orders WHERE status IN ('PENDING', 'ACCEPTED', 'COOKING', 'READY') ORDER BY created_at ASC",
                ROW_MAPPER
        );
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update(
                "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?",
                status, LocalDateTime.now().toString(), id
        );
    }

    public int countByStatus(String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = ?", Integer.class, status
        );
        return count != null ? count : 0;
    }

    public List<Order> findByCreatedAtAfter(LocalDateTime after) {
        return jdbcTemplate.query(
                "SELECT * FROM orders WHERE created_at >= ? ORDER BY created_at DESC",
                ROW_MAPPER, after.toString()
        );
    }
}
