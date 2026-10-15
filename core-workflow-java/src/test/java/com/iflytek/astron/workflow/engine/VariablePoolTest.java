package com.iflytek.astron.workflow.engine;

import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/12/5
 */
public class VariablePoolTest {

    @org.junit.jupiter.api.Test
    void basicTest() {
        VariablePool variablePool = new VariablePool();
        // 存储基本数据类型
        variablePool.set("node-start::001", "user_input", "请介绍一下Java");
        variablePool.set("node-start::001", "request_id", 12345);
        variablePool.set("node-start::001", "is_urgent", true);

// 提取基本数据类型
        String userInput = (String) variablePool.get("node-start::001", "user_input");
        Integer requestId = (Integer) variablePool.get("node-start::001", "request_id");
        Boolean isUrgent = (Boolean) variablePool.get("node-start::001", "is_urgent");
        System.out.println("用户输入：" + userInput);
        System.out.println("请求ID：" + requestId);
        System.out.println("是否紧急：" + isUrgent);
    }

    @Data
    public static class LLMResponse {
        private String content;
        private Integer wordCount;
        private List<String> keywords;
        private Metadata metadata;
    }

    @Data
    public class Metadata {
        private String model;
        private Double temperature;
        private Long timestamp;
    }

    @org.junit.jupiter.api.Test
    public void objTest() {
        // 1. 存储复杂对象（自动转换为JSONObject）
        LLMResponse response = new LLMResponse();
        response.setContent("Java是一种面向对象的编程语言...");
        response.setWordCount(150);
        response.setKeywords(List.of("Java", "OOP", "编程"));
        Metadata metadata = new Metadata();
        metadata.setModel("deepseek-chat");
        metadata.setTemperature(0.7D);
        metadata.setTimestamp(System.currentTimeMillis());
        response.setMetadata(metadata);

        VariablePool variablePool = new VariablePool();
        variablePool.set("node-llm::002", "response", response);

// 2. 直接引用对象成员
        String content = (String) variablePool.get("node-llm::002", "response.content");
        Integer wordCount = (Integer) variablePool.get("node-llm::002", "response.wordCount");
        String model = (String) variablePool.get("node-llm::002", "response.metadata.model");
        var temperature = variablePool.get("node-llm::002", "response.metadata.temperature");
        System.out.println("内容：" + content);
        System.out.println("字数：" + wordCount);
        System.out.println("模型：" + model);
        System.out.println("温度：" + temperature);
    }

    public record VoiceSegment(String text, String speaker, Integer duration) {
    }

    @org.junit.jupiter.api.Test
    public void listTest() {
        // 存储包含数组的对象
        List<VoiceSegment> segments = Arrays.asList(
                new VoiceSegment("欢迎收听技术播客", "xiaoyan", 3000),
                new VoiceSegment("今天我们要讨论Java", "yihui", 2500)
        );
        Map<String, Object> ttsResult = new HashMap<>();
        ttsResult.put("segments", segments);
        ttsResult.put("total_duration", 5500);

        VariablePool variablePool = new VariablePool();
        variablePool.set("node-tts::003", "result", ttsResult);

        // 通过索引引用数组元素的属性
        String firstSegmentText = (String) variablePool.get("node-tts::003", "result.segments[0].text");
        String secondSpeaker = (String) variablePool.get("node-tts::003", "result.segments[1].speaker");
        Integer firstDuration = (Integer) variablePool.get("node-tts::003", "result.segments[0].duration");
        System.out.println("第一段文本：" + firstSegmentText);
        System.out.println("第二段发言人：" + secondSpeaker);
        System.out.println("第一段时长：" + firstDuration);
    }
}