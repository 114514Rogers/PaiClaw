package com.iflytek.astron.workflow.engine.constants;

import lombok.Getter;

/**
 * Enumeration for end node output modes
 * 
 * @author 小陈财话会
 * @version 1.0.0
 */
@Getter
public enum EndNodeOutputModeEnum {
    DIRECT_MODE("direct", 0),
    VARIABLE_MODE("variable", 1);

    private final String value;
    private final Integer mode;

    EndNodeOutputModeEnum(String value, Integer mode) {
        this.value = value;
        this.mode = mode;
    }

    public static EndNodeOutputModeEnum fromValue(String value) {
        for (EndNodeOutputModeEnum mode : EndNodeOutputModeEnum.values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return DIRECT_MODE;
    }

    @Override
    public String toString() {
        return value;
    }
}