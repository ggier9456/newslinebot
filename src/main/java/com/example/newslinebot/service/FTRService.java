package com.example.newslinebot.service;

import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
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
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import java.util.*;

@Service
public class FTRService {

    // 使用針對新聞摘要優化的輕量模型
    private static final String MODEL_ID = "google/pegasus-cnn_dailymail";
    private static final String API_URL = "https://router.huggingface.co/hf-inference/models/" + MODEL_ID;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // 加入 ObjectMapper

    public FTRService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void getNews() {
        String url = "http://localhost:8081/makefulltextfeed.php?url=https://techcrunch.com/tag/AI/feed/&format=json";
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        Map<String, Object> rss = (Map<String, Object>) response.get("rss");
        Map<String, Object> channel = (Map<String, Object>) rss.get("channel");
        List<Map<String, Object>> items = (List<Map<String, Object>>) channel.get("item");
        int i = 0;
        for(Map<String, Object> item : items) {
            if(i > 0) break;

            System.out.println("title: " + item.get("title"));
            System.out.println("guID: " + item.get("guid"));
            System.out.println("description: " );
            String text = parseHtml((String) item.get("description"));
            String summary = huggingFaceSummarizer(text);
            System.out.println("summary: " + summary);
            System.out.println("pubDate: " + parseDate((String) item.get("pubDate")));
            System.out.println("category: " + item.get("category"));
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

    public String parseDate(String rfcDate) {
        DateTimeFormatter rfcFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(rfcDate, rfcFormatter);

            // 轉換成台灣時間 (UTC+8)
            ZonedDateTime taiwanTime = zdt.withZoneSameInstant(ZoneId.of("Asia/Taipei"));

            // 格式化成 YYYY-MM-DD HH:mm:ss
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formatted = taiwanTime.format(outputFormatter);

            return formatted;
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String huggingFaceSummarizer(String news){
        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(API_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 建構請求本體
        Map<String, Object> body = new HashMap<>();
        String prompt = "Summarize the following news article in bullet points:\n\n" + news;
        body.put("inputs", prompt);

        // 可選參數：控制摘要長度
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("min_length", 300); // 摘要最短長度
        parameters.put("max_length", 800); // 摘要最長長度
        parameters.put("do_sample", false); // false 通常比較穩定
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // 關鍵修改：改用 String.class 接收回應，避開 MediaType 解析錯誤
            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 手動解析 JSON 字串
                // Hugging Face 回傳的是一個陣列: [{"summary_text": "..."}]
                List<Map<String, Object>> resultList = objectMapper.readValue(
                        response.getBody(),
                        List.class
                );

                if (!resultList.isEmpty()) {
                    return (String) resultList.get(0).get("summary_text");
                }
            }
        } catch (Exception e) {
            // ... 錯誤處理 ...
            e.printStackTrace();
        }
        return "摘要生成失敗";
    }
}
