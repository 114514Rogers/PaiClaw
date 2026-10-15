package com.iflytek.astron.workflow.engine.constants;

/**
 * @author YiHui
 * @date 2025/12/2
 */
public enum MsgTypeEnum {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    THINKING("thinking");
    private String type;
    MsgTypeEnum(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
}
