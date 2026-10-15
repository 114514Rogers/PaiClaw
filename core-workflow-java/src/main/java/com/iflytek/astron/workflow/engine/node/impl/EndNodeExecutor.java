package com.iflytek.astron.workflow.engine.node.impl;

import com.iflytek.astron.workflow.engine.constants.EndNodeOutputModeEnum;
import com.iflytek.astron.workflow.engine.constants.NodeExecStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.node.AbstractNodeExecutor;
import com.iflytek.astron.workflow.engine.util.VariableTemplateRender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * End node executor
 * Formats the final output using a template
 */
@Slf4j
@Component
public class EndNodeExecutor extends AbstractNodeExecutor {

    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.END;
    }

    @Override
    protected NodeRunResult executeNode(NodeState node, Map<String, Object> inputs) {
        Map<String, Object> nodeParam = node.node().getData().getNodeParam();

        // 返回结果有两种定义
        // 2. 直接返回
        // 1. 自定义格式进行返回
        Integer outputMode = getOutputMode(nodeParam);

        String finalOutput;
        String finalReason = "";

        if (Objects.equals(outputMode, EndNodeOutputModeEnum.VARIABLE_MODE.getMode())) {
            String template = getTemplate(nodeParam);
            if (!StringUtils.isEmpty(template)) {
                finalOutput = VariableTemplateRender.render(template, inputs);
                log.info("End node: formatted output using template (length={})", finalOutput.length());
            } else {
                finalOutput = toStr(inputs);
            }

            String reasoningTemplate = getReasonTemplate(nodeParam);
            if (!StringUtils.isEmpty(reasoningTemplate)) {
                finalReason = VariableTemplateRender.render(reasoningTemplate, inputs);
            }
        } else {
            finalOutput = toStr(inputs);
        }

        Map<String, Object> outputs = new HashMap<>();
        // 输出结果
        outputs.put("content", finalOutput);
        // 输出的思考过程
        outputs.put("reasoning_content", finalReason);

        NodeRunResult result = new NodeRunResult();
        result.setInputs(inputs);
        result.setOutputs(outputs);
        result.setStatus(NodeExecStatusEnum.SUCCESS);
        return result;
    }

    private Integer getOutputMode(Map<String, Object> nodeParam) {
        Object outputModeObj = nodeParam.get("outputMode");
        if (outputModeObj instanceof Integer) {
            return (Integer) outputModeObj;
        } else if (outputModeObj instanceof Number) {
            return ((Number) outputModeObj).intValue();
        }
        return 1;
    }

    private String getTemplate(Map<String, Object> nodeParam) {
        Object templateObj = nodeParam.get("template");
        return templateObj != null ? String.valueOf(templateObj) : "";
    }

    private String getReasonTemplate(Map<String, Object> nodeParam) {
        Object templateObj = nodeParam.get("reasoningTemplate");
        return templateObj != null ? String.valueOf(templateObj) : "";
    }

    private String toStr(Map<String, Object> inputs) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
