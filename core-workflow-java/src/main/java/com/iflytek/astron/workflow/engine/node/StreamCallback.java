package com.iflytek.astron.workflow.engine.node;

/**
 * Callback interface for streaming node execution output
 * Used for SSE (Server-Sent Events) real-time progress updates
 */
public interface StreamCallback {

    /**
     * Send a streaming message to the client
     *
     * @param eventType event type (e.g., "node_start", "node_output", "node_end", "error")
     * @param data      event data (will be serialized to JSON)
     */
    void callback(String eventType, Object data);

    default void finished() {
    }
}
