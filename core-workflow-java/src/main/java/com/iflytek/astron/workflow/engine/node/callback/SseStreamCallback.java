package com.iflytek.astron.workflow.engine.node.callback;

import com.alibaba.fastjson2.JSON;
import com.iflytek.astron.workflow.engine.node.StreamCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
public class SseStreamCallback implements StreamCallback {

    private final SseEmitter emitter;

    public SseStreamCallback(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void callback(String eventType, Object data) {
        try {
            String jsonData = JSON.toJSONString(data);
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(jsonData));
            if (log.isDebugEnabled()) {
                log.info("response data to client： {}", jsonData);
            }
        } catch (IOException e) {
            log.error("Failed to send SSE event: {}", e.getMessage(), e);
        }
    }

    @Override
    public void finished() {
    }
}