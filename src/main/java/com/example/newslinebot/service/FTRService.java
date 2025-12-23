package com.example.newslinebot.service;

import com.example.newslinebot.DAO.NewsCategoryDAO;
import com.example.newslinebot.DAO.NewsDAO;
import com.example.newslinebot.DAO.NewsToCategoryDAO;
import com.example.newslinebot.Model.News;
import com.example.newslinebot.Model.NewsCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.in;

@Service
public class FTRService {

    @Autowired
    NewsDAO newsDAO;
    @Autowired
    NewsCategoryDAO newsCategoryDAO;
    @Autowired
    NewsToCategoryDAO newsToCategoryDAO;

    // 使用針對新聞摘要優化的輕量模型
    @Value("${huggingface.api-key}")
    private String API_TOKEN;
    private static final String API_URL = "https://router.huggingface.co/v1/chat/completions";
    private static final String NEWS_URL = "http://localhost:8081/makefulltextfeed.php?url=https://techcrunch.com/tag/Google/feed/&format=json";
//    private static final String NEWS_URL = "http://localhost:8081/makefulltextfeed.php?url=https://techcrunch.com/feed/&format=json";

    private final RestTemplate restTemplate;

    public FTRService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void getNews() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(NEWS_URL, Map.class);
        Map<String, Object> rss = (Map<String, Object>) response.get("rss");
        Map<String, Object> channel = (Map<String, Object>) rss.get("channel");
        List<Map<String, Object>> items = (List<Map<String, Object>>) channel.get("item");


        int i = 0;
        for(Map<String, Object> item : items) {
            if(i > 1) break;
            List<String> categories = (List<String>) item.get("category");
            System.out.println("第" +(i+1) + "篇新聞");
            System.out.println("title: " + item.get("title"));
            System.out.println("guID: " + (String)item.get("guid"));
            System.out.println("description: " );
            String text = parseHtml((String) item.get("description"));
            String summary = huggingFaceSummarizer(text);
            System.out.println("summary: " + summary);
            System.out.println("pubDate: " + parseDate((String) item.get("pubDate")));
            System.out.println("category: " + item.get("category"));

            //新增新聞資料
            Integer guid = parseNewsGUID((String)item.get("guid"));
            if(newsDAO.findByGuid(guid) != null) {continue;}
            News news = new News();
            news.setTitle((String)item.get("title"));
            news.setGuid(guid);
            news.setNewsUrl((String)item.get("guid"));
            news.setSummary(summary);
            news.setPubDate(parseDate((String) item.get("pubDate")));
            newsDAO.insert(news);

            //新增新聞類別資料
            for(String c : categories){
                if(newsCategoryDAO.findByCategory(c) != null){ continue;}
                newsCategoryDAO.insert(c);
            }

            //新增新聞與新聞類別關聯資料
            for(String c : categories){
                NewsCategory newsCategory = newsCategoryDAO.findByCategory(c);
                if(newsToCategoryDAO.findByNewsIdAndCategoryId(guid, newsCategory.getCId()) != null){ continue;}
                newsToCategoryDAO.insert(guid, newsCategory.getCId());
            }
            i++;
        }
    }

    public String parseHtml(String html) {
        String text = "";
        Document doc = Jsoup.parse(html);
        Elements paragraphs = doc.select("p.wp-block-paragraph");
        for (Element p : paragraphs) {
            System.out.println(p.text());
            text += p.text() + "\n";
        }
        return text;
    }

    public LocalDateTime parseDate(String rfcDate) {
        DateTimeFormatter rfcFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(rfcDate, rfcFormatter);

            // 轉成台灣時區
            ZonedDateTime taiwanTime =
                    zdt.withZoneSameInstant(ZoneId.of("Asia/Taipei"));

            // 轉成 LocalDateTime（沒有時區）
            return taiwanTime.toLocalDateTime();

        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String huggingFaceSummarizer(String news){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // messages 內容
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        String prompt = "說明一下這篇新聞在說什麼 請用繁體中文 並且將你認為的重點條列式說明(大概5點就好): " + news;
        message.put("content", prompt);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message);

        // 參數
        Map<String, Object> body = new HashMap<>();
        body.put("model", "Qwen/Qwen3-4B-Instruct-2507:nscale");
        body.put("messages", messages);
        body.put("stream", false);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);
            Map responseBody = response.getBody();

            // Hugging Face 回傳格式取 choices[0].message.content
            List choices = (List) responseBody.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map messageObj = (Map) firstChoice.get("message");

            return (String) messageObj.get("content");
        } catch (Exception e) {
            // ... 錯誤處理 ...
            e.printStackTrace();
        }
        return "摘要生成失敗";
    }

    public Integer parseNewsGUID(String url){
        Pattern pattern = Pattern.compile("[?&]p=(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }
}
