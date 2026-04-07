package com.restaurant.order.repository;

import com.restaurant.order.dto.BestSellerResponse;
import com.restaurant.order.dto.RevenueResponse;
import com.restaurant.order.entity.OrderItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class OrderItemRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<OrderItem> ROW_MAPPER = (rs, rowNum) -> {
        OrderItem item = new OrderItem();
        item.setId(rs.getLong("id"));
        item.setOrderId(rs.getLong("order_id"));
        item.setMenuId(rs.getLong("menu_id"));
        item.setMenuName(rs.getString("menu_name"));
        item.setPrice(rs.getInt("price"));
        item.setQuantity(rs.getInt("quantity"));
        return item;
    };

    public OrderItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OrderItem save(OrderItem item) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO order_items (order_id, menu_id, menu_name, price, quantity) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, item.getOrderId());
            ps.setLong(2, item.getMenuId());
            ps.setString(3, item.getMenuName());
            ps.setInt(4, item.getPrice());
            ps.setInt(5, item.getQuantity());
            return ps;
        }, keyHolder);
        item.setId(keyHolder.getKey().longValue());
        return item;
    }

    public List<OrderItem> findByOrderId(Long orderId) {
        return jdbcTemplate.query(
                "SELECT * FROM order_items WHERE order_id = ?", ROW_MAPPER, orderId
        );
    }

    public List<RevenueResponse> getRevenueByDay(String from, String to) {
        return jdbcTemplate.query(
                "SELECT DATE(o.created_at) AS period, " +
                        "SUM(oi.price * oi.quantity) AS total_revenue, " +
                        "COUNT(DISTINCT o.id) AS order_count " +
                        "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                        "WHERE DATE(o.created_at) >= ? AND DATE(o.created_at) <= ? " +
                        "GROUP BY DATE(o.created_at) ORDER BY period",
                (rs, rowNum) -> new RevenueResponse(
                        rs.getString("period"),
                        rs.getInt("total_revenue"),
                        rs.getInt("order_count")
                ), from, to
        );
    }

    public List<RevenueResponse> getRevenueByWeek(String from, String to) {
        return jdbcTemplate.query(
                "SELECT strftime('%Y-W%W', o.created_at) AS period, " +
                        "SUM(oi.price * oi.quantity) AS total_revenue, " +
                        "COUNT(DISTINCT o.id) AS order_count " +
                        "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                        "WHERE DATE(o.created_at) >= ? AND DATE(o.created_at) <= ? " +
                        "GROUP BY strftime('%Y-W%W', o.created_at) ORDER BY period",
                (rs, rowNum) -> new RevenueResponse(
                        rs.getString("period"),
                        rs.getInt("total_revenue"),
                        rs.getInt("order_count")
                ), from, to
        );
    }

    public List<RevenueResponse> getRevenueByMonth(String from, String to) {
        return jdbcTemplate.query(
                "SELECT strftime('%Y-%m', o.created_at) AS period, " +
                        "SUM(oi.price * oi.quantity) AS total_revenue, " +
                        "COUNT(DISTINCT o.id) AS order_count " +
                        "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                        "WHERE DATE(o.created_at) >= ? AND DATE(o.created_at) <= ? " +
                        "GROUP BY strftime('%Y-%m', o.created_at) ORDER BY period",
                (rs, rowNum) -> new RevenueResponse(
                        rs.getString("period"),
                        rs.getInt("total_revenue"),
                        rs.getInt("order_count")
                ), from, to
        );
    }

    public List<BestSellerResponse> getBestSellers(int limit, String from, String to) {
        return jdbcTemplate.query(
                "SELECT oi.menu_id, oi.menu_name, " +
                        "SUM(oi.quantity) AS total_quantity, " +
                        "SUM(oi.price * oi.quantity) AS total_revenue " +
                        "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                        "WHERE DATE(o.created_at) >= ? AND DATE(o.created_at) <= ? " +
                        "GROUP BY oi.menu_id, oi.menu_name " +
                        "ORDER BY total_quantity DESC LIMIT ?",
                (rs, rowNum) -> new BestSellerResponse(
                        rs.getLong("menu_id"),
                        rs.getString("menu_name"),
                        rs.getInt("total_quantity"),
                        rs.getInt("total_revenue")
                ), from, to, limit
        );
    }

    public List<BestSellerResponse> getBestSellersForDays(int days, int limit) {
        return jdbcTemplate.query(
                "SELECT oi.menu_id, oi.menu_name, " +
                        "SUM(oi.quantity) AS total_quantity, " +
                        "SUM(oi.price * oi.quantity) AS total_revenue " +
                        "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                        "WHERE o.created_at >= datetime('now', '-' || ? || ' days') " +
                        "GROUP BY oi.menu_id, oi.menu_name " +
                        "ORDER BY total_quantity DESC LIMIT ?",
                (rs, rowNum) -> new BestSellerResponse(
                        rs.getLong("menu_id"),
                        rs.getString("menu_name"),
                        rs.getInt("total_quantity"),
                        rs.getInt("total_revenue")
                ), days, limit
        );
    }
}
