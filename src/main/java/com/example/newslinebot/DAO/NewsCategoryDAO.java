package com.example.newslinebot.DAO;

import com.example.newslinebot.Model.News;
import com.example.newslinebot.Model.NewsCategory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NewsCategoryDAO {
    private final JdbcTemplate jdbcTemplate;

    public NewsCategoryDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(String category) {
        String sql = """
                INSERT INTO news_category (
                    category, 
                    created_at, 
                    updated_at
                ) VALUES (?, NOW(), NOW())
                """;
        return jdbcTemplate.update(sql, category);
    }


    public NewsCategory findByCategory(String category) {
        String sql = """
                SELECT
                    c_id,
                    category
                FROM news_category
                WHERE category = ?
                """;
        List<NewsCategory> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(NewsCategory.class), category);
        return list.isEmpty() ? null : list.get(0);
    }
}
