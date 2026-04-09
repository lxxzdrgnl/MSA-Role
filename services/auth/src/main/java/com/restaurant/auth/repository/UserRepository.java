package com.restaurant.auth.repository;

import com.restaurant.auth.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setRole(rs.getString("role"));
        return user;
    };

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByEmail(String email) {
        var list = jdbc.query("SELECT * FROM users WHERE email = ?", rowMapper, email);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findById(Long id) {
        var list = jdbc.query("SELECT * FROM users WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public User save(User user) {
        jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES (?, ?, ?, ?)",
                user.getEmail(), user.getPassword(), user.getNickname(), user.getRole());
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        user.setId(id);
        return user;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public void updateRole(Long id, String role) {
        jdbc.update("UPDATE users SET role = ? WHERE id = ?", role, id);
    }

    public void updateNickname(Long id, String nickname) {
        jdbc.update("UPDATE users SET nickname = ? WHERE id = ?", nickname, id);
    }

    public Map<Long, String> findNicknamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, nickname FROM users WHERE id IN (" + placeholders + ")";
        Map<Long, String> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getLong("id"), rs.getString("nickname"));
        }, ids.toArray());
        return result;
    }
}
