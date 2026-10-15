package com.iflytek.astron.workflow.engine.constants;

import lombok.Getter;

/**
 * node执行状态枚举
 *
 * @author YiHui
 * @date 2025/12/2
 */
@Getter
public enum NodeExecStatusEnum {
    SUCCESS(true, "成功"),
    MANUAL_INTERRUPT(false, "成功，需要人工介入进行交互"),
    ERR_RETRY(false, "失败，可重试"),
    ERR_INTERUPT(false, "中断流程"),
    ERR_CODE_MSG(false, "失败，返回预设错误码和错误信息，走正常分支"),
    ERR_FAIL_CONDITION(false, "失败，走失败分支"),
    ;

    private boolean success;
    private String desc;

    NodeExecStatusEnum(boolean success, String desc) {
        this.success = success;
        this.desc = desc;
    }
}
