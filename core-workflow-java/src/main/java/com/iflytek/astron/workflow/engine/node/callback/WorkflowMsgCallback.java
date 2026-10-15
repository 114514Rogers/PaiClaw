package com.iflytek.astron.workflow.engine.node.callback;

import com.iflytek.astron.workflow.engine.constants.EndNodeOutputModeEnum;
import com.iflytek.astron.workflow.engine.domain.NodeRunResult;
import com.iflytek.astron.workflow.engine.domain.callbacks.ChatCallBackStreamResult;
import com.iflytek.astron.workflow.engine.domain.callbacks.ChatCallBacks;
import com.iflytek.astron.workflow.engine.domain.callbacks.LLMGenerate;
import com.iflytek.astron.workflow.engine.node.StreamCallback;
import com.iflytek.astron.workflow.engine.util.AsyncUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Workflow stream callback implementation that bridges ChatCallBacks with StreamCallback
 * This class handles workflow events and forwards them to the underlying StreamCallback
 */
@Slf4j
public class WorkflowMsgCallback implements StreamCallback {

    private final ChatCallBacks chatCallBacks;
    private final StreamCallback clientCallback;

    private volatile boolean tag = true;
    private volatile boolean scheduleTaskOver = false;

    public WorkflowMsgCallback(String sid,
                               StreamCallback clientCallback,
                               EndNodeOutputModeEnum endNodeOutputMode,
                               Queue<LLMGenerate> streamQueue,
                               Queue<ChatCallBackStreamResult> needOrderStreamResultQ) {
        this.clientCallback = clientCallback;
        this.chatCallBacks = new ChatCallBacks(
                sid,
                streamQueue, // streamQueue is handled internally
                endNodeOutputMode,
                Set.of(),
                needOrderStreamResultQ
        );


        // 写一个任务，用于从 streamQueue 获取数据，发送给用户
        AsyncUtil.execute(() -> {
            while (tag) {
                try {
                    // 使用阻塞方式等待数据
                    while (streamQueue.isEmpty() && tag) {
                        AsyncUtil.sleep(10); // 短暂休眠避免忙等待
                    }

                    LLMGenerate resp = streamQueue.poll();
                    if (resp != null) {
                        clientCallback.callback("stream", resp);
                    }
                } catch (Exception e) {
                    log.error("Error in stream callback", e);
                }
            }
            scheduleTaskOver = true;
        });
    }

    /**
     * Handle workflow start event
     */
    public void onWorkflowStart() {
        chatCallBacks.onSparkflowStart();
    }

    /**
     * Handle workflow end event
     *
     * @param message Final node run result containing execution summary
     */
    public void onWorkflowEnd(NodeRunResult message) {
        chatCallBacks.onSparkflowEnd(message);
    }

    /**
     * Handle node start event
     *
     * @param code      Status code for the node start operation
     * @param nodeId    Unique identifier of the starting node
     * @param aliasName Human-readable name for the node
     */
    public void onNodeStart(int code, String nodeId, String aliasName) {
        chatCallBacks.onNodeStart(code, nodeId, aliasName);
    }

    /**
     * Handle node processing event
     *
     * @param code             Status code for the node processing operation
     * @param nodeId           Unique identifier of the processing node
     * @param aliasName        Human-readable name for the node
     * @param message          Processing message or error content
     * @param reasoningContent Additional reasoning or intermediate content
     */
    public void onNodeProcess(int code, String nodeId, String aliasName,
                              String message, String reasoningContent) {
        chatCallBacks.onNodeProcess(code, nodeId, aliasName, message, reasoningContent);
    }

    /**
     * Handle node interrupt event
     *
     * @param eventId      Unique identifier for the interrupt event
     * @param value        Interrupt event data
     * @param nodeId       Unique identifier of the interrupted node
     * @param aliasName    Human-readable name for the node
     * @param code         Status code for the interrupt operation
     * @param finishReason Reason for the interrupt
     * @param needReply    Whether a reply is needed for the interrupt
     */
    public void onNodeInterrupt(String eventId, Map<String, Object> value,
                                String nodeId, String aliasName, int code,
                                String finishReason, boolean needReply) {
        chatCallBacks.onNodeInterrupt(eventId, value, nodeId, aliasName, code, finishReason, needReply);
    }

    /**
     * Handle node end event
     *
     * @param nodeId    Unique identifier of the completed node
     * @param aliasName Human-readable name for the node
     * @param message   Node execution result, null if execution failed
     */
    public void onNodeEnd(String nodeId, String aliasName,
                          NodeRunResult message) {
        chatCallBacks.onNodeEnd(nodeId, aliasName, message, message.getError());
    }

    @Override
    public void callback(String eventType, Object data) {
        clientCallback.callback(eventType, data);
    }


    public void finished() {
        tag = false;
        while (!scheduleTaskOver) {
            AsyncUtil.sleep(10);
        }

        // 需要等待异步线程任务执行完毕，然后再执行下面的内容
        while (!chatCallBacks.getStreamQueue().isEmpty()) {
            var resp = chatCallBacks.getStreamQueue().poll();
            callback("stream", resp);
        }

        while (!chatCallBacks.getOrderStreamResultQ().isEmpty()) {
            var resp = chatCallBacks.getOrderStreamResultQ().poll();
            clientCallback.callback("stream", resp.getNodeAnswerContent());
        }

        clientCallback.finished();
    }

    /**
     * Handle end node executed event
     */
    public void onEndNodeExecuted(String nodeId, String aliasName, NodeRunResult message) {
        message.setNodeAnswerContent((String) message.getOutputs().getOrDefault("content", ""));
        message.setNodeAnswerReasoningContent((String) message.getOutputs().getOrDefault("reasoning_content", ""));
        message.setOutputs(message.getInputs());
        message.setInputs(Map.of());
        onNodeEnd(nodeId, aliasName, message);
    }

    /**
     * Handle start node executed event
     */
    public void onStartNodeExecuted(String nodeId, String aliasName, NodeRunResult message) {
        message.setOutputs(Map.of());
        this.onNodeEnd(nodeId, aliasName, message);
    }
}