package com.iflytek.astron.workflow.engine.integration.model.bo;

import org.springframework.ai.chat.metadata.Usage;

/**
 * @author YiHui
 * @date 2025/12/1
 */
public record LlmResVo(Usage usage, String content, String thinkContent) {
}
