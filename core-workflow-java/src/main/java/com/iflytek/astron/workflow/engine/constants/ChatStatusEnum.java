package com.iflytek.astron.workflow.engine.constants;

import lombok.Getter;

/**
 * 会话状态
 *
 * @author YiHui
 * @date 2025/12/2
 */
@Getter
public enum ChatStatusEnum {
    PING("ping"),
    RUNNING("running"),
    STOP("stop"),
    ERROR("interrupt"),
    ;

    private String status;

    ChatStatusEnum(String status) {
        this.status = status;
    }
}
