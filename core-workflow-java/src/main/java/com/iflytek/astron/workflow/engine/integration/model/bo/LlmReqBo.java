package com.iflytek.astron.workflow.engine.integration.model.bo;

import com.iflytek.astron.workflow.engine.integration.model.LlmChatHistory;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * llm请求信息
 *
 * @author YiHui
 * @date 2025/12/1
 */
@Data
public class LlmReqBo {
    private String nodeId;

    /**
     * 模型id
     */
    private String modelId;

    /**
     * 用户输入
     */
    private String userMsg;

    /**
     * 系统提示词
     */
    private String systemMsg;

    private String model;

    private String url;

    private String apiKey;

    private String apiSecret;

    private String source;


    private Integer topK;

    private Integer maxTokens;

    private Boolean isThink;

    private Boolean multiMode;

    private Boolean modelEnabled;

    private Map<String, Object> extraParams;

    private List<LlmChatHistory.ChatItem> history;
}
