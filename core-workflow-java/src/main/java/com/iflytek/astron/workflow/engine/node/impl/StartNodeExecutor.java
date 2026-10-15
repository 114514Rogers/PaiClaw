package com.iflytek.astron.workflow.engine.node.impl;

import com.iflytek.astron.workflow.engine.constants.NodeExecStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.node.AbstractNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Start node executor
 * Simply passes through the initial inputs to outputs
 */
@Slf4j
@Component
public class StartNodeExecutor extends AbstractNodeExecutor {

    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.START;
    }

    @Override
    protected NodeRunResult executeNode(NodeState nodeState, Map<String, Object> inputs) {
        Map<String, Object> outputs = new HashMap<>(inputs);

        NodeRunResult result = new NodeRunResult();
        result.setInputs(inputs);
        result.setOutputs(outputs);
        result.setStatus(NodeExecStatusEnum.SUCCESS);
        return result;
    }
}
