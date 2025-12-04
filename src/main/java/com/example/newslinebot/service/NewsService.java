package com.example.newslinebot.service;

import com.example.newslinebot.DTO.NewsArticleDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {
    @Value("${news-api.api-key}")
    private String apiKey;

    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";

    public void getTodayTechNews() {
        String from = "2025-12-01";
        String to = "2025-12-04";
        String query = "technology OR tech OR gadgets OR artificial intelligence OR ChatGPT OR OpenAI OR NVIDIA OR AGI OR programming OR developer OR software OR coding OR backend OR frontend OR database";
        String sources = String.join(",",
                "the-verge",
                "techcrunch",
                "wired",
                "ars-technica",
                "engadget",
                "new-scientist",
                "techradar"
        );


        String url = "https://newsapi.org/v2/everything"
                + "?sources=" + sources
                + "&from=" + from
                + "&to=" + to
                + "&sortBy=publishedAt"
                + "&language=en"
                + "&pageSize=5"
                + "&apiKey=" + apiKey;
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        System.out.println("完整 response: " + response);

        List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");

        for (Map<String, Object> article : articles) {
            NewsArticleDTO newsArticleDTO = new NewsArticleDTO();
            newsArticleDTO.setTitle((String) article.get("title"));
            newsArticleDTO.setDescription((String) article.get("description"));
            newsArticleDTO.setUrl((String) article.get("url"));
            newsArticleDTO.setPublishedAt((String) article.get("publishedAt"));
            System.out.println("Title: " + article.get("title"));
            System.out.println("Description: " + article.get("description"));
            System.out.println("URL: " + article.get("url"));
            System.out.println("PublishedAt: " + article.get("publishedAt"));
            System.out.println("-----");
        }
    }
}
