package com.game.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.List;
import software.amazon.awssdk.core.SdkBytes;

@Service
@Slf4j
public class ImageGenerationService {
    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.bedrock.model-id}")
    private String modelId;

    public ImageGenerationService(BedrockRuntimeClient bedrockClient) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = new ObjectMapper();
    }

    public String generateImage(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.error("Empty prompt provided");
            return null;
        }

        try {
            // Escape special characters in the prompt
            String escapedPrompt = prompt.replace("\"", "\\\"")
                                      .replace("\n", " ")
                                      .replace("\r", " ");

            // Create the request body according to Titan's exact format
            Map<String, Object> requestMap = Map.of(
                "taskType", "TEXT_TO_IMAGE",
                "textToImageParams", Map.of(
                    "text", escapedPrompt,
                    "negativeText", "blurry, low quality, distorted, deformed",
                    "imageGenerationConfig", Map.of(
                        "height", 1024,
                        "width", 1024,
                        "numberOfImages", 1,
                        "guidanceScale", 8.0,
                        "seed", 0
                    )
                )
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);
            
            log.info("Request body: {}", requestBody);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId("amazon.titan-image-generator-v1")
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            if (response == null || response.body() == null) {
                log.error("Null response from Bedrock");
                return null;
            }

            Map<String, Object> responseMap = objectMapper.readValue(
                response.body().asByteArray(), Map.class);
            
            log.debug("Received response from Bedrock: {}", responseMap);
            
            if (responseMap.containsKey("error")) {
                log.error("Error from Bedrock: {}", responseMap.get("error"));
                return null;
            }

            List<Map<String, Object>> images = (List<Map<String, Object>>) responseMap.get("images");
            if (images == null || images.isEmpty()) {
                log.error("No images in response");
                return null;
            }

            String base64Image = (String) images.get(0).get("base64");
            if (base64Image == null) {
                log.error("No base64 image in response");
                return null;
            }

            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            log.error("Failed to generate image: {}", e.getMessage(), e);
            return null;
        }
    }
}
