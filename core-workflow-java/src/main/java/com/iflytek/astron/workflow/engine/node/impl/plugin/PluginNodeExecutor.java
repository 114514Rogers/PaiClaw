package com.iflytek.astron.workflow.engine.node.impl.plugin;

import com.iflytek.astron.workflow.engine.constants.NodeExecStatusEnum;
import com.iflytek.astron.workflow.engine.constants.NodeTypeEnum;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.NodeState;
import com.iflytek.astron.workflow.engine.integration.plugins.PluginServiceClient;
import com.iflytek.astron.workflow.engine.node.AbstractNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Plugin node executor
 * Calls external plugin/tool services (e.g., voice synthesis via core-aitools)
 */
@Slf4j
@Component
public class PluginNodeExecutor extends AbstractNodeExecutor {
    @Autowired
    private PluginServiceClient pluginServiceClient;

    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.PLUGIN;
    }

    @Override
    protected NodeRunResult executeNode(NodeState nodeState, Map<String, Object> inputs) throws Exception {
        Map<String, Object> outputs = pluginServiceClient.toolCall(nodeState, inputs);
        NodeRunResult result = new NodeRunResult();
        result.setOutputs(outputs);
        result.setInputs(inputs);
        result.setStatus(NodeExecStatusEnum.SUCCESS);
        return result;
    }
}