package com.iflytek.astron.workflow.engine.util;

import java.util.UUID;

/**
 * @author YiHui
 * @date 2025/12/2
 */
public class FlowUtil {

    public static String genWorkflowId(String flow) {
        return "workflow#" + flow + "-" + System.currentTimeMillis();
    }


    public static String genSid() {
        return UUID.randomUUID().toString();
    }

    public static String genInterruptEventId() {
        return "interrupt";
    }
}
