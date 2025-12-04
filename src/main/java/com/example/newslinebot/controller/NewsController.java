package com.example.newslinebot.controller;

import com.example.newslinebot.DTO.NewsArticleDTO;
import com.example.newslinebot.service.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/news/today")
    public void getTodayNews() {
        newsService.getTodayTechNews();
    }
}
