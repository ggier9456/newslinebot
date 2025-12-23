package com.example.newslinebot.Model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewsCategory {
    private Integer cId;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
