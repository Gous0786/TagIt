package com.game.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class MemeService {

    private static final String GET_MEMES_URL = "https://api.imgflip.com/get_memes";

    public List<Map<String, Object>> fetchMemes() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(GET_MEMES_URL, Map.class);

        if (response != null && Boolean.TRUE.equals(response.get("success"))) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return (List<Map<String, Object>>) data.get("memes");
        }
        return List.of();
    }
} 