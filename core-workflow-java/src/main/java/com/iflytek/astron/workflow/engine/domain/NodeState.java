package com.iflytek.astron.workflow.engine.domain;

import com.iflytek.astron.workflow.engine.VariablePool;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.engine.node.callback.WorkflowMsgCallback;

/**
 * node之间执行时传递的状态信息
 *
 * @author YiHui
 * @date 2025/12/3
 */
public record NodeState(Node node, VariablePool variablePool, WorkflowMsgCallback callback) {
}
