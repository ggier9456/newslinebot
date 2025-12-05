package com.example.newslinebot.DTO;
import lombok.Data;

@Data
public class NewsArticleDTO {
    private String title;
    private String description;
    private String url;
    private String publishedAt;

}
