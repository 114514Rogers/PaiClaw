package com.iflytek.astron.workflow.engine.integration.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iflytek.astron.workflow.engine.constants.MsgTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * LLM聊天历史记录缓存
 * Key: ChatId + NodeId组合
 * Value: 固定长度的对话历史记录列表
 *
 * @author YiHui
 * @date 2025/12/2
 */
public class LlmChatHistory {

    /**
     * 聊天历史记录缓存
     * Key格式: "{chatId}:{nodeId}"
     * Value: 固定长度的历史记录队列
     */
    private static final LoadingCache<String, ConcurrentLinkedQueue<ChatItem>> chatHistoryCache =
            CacheBuilder.newBuilder()
                    .maximumSize(10000) // 最大缓存10000个会话
                    .expireAfterWrite(30, TimeUnit.MINUTES) // 30分钟后过期
                    .build(CacheLoader.from(LlmChatHistory::createChatHistoryQueue));

    /**
     * 聊天历史记录的最大长度
     */
    private static final int MAX_HISTORY_LENGTH = 10;

    /**
     * 聊天消息实体类，使用record类型
     */
    public record ChatMessage(MsgTypeEnum role, String content, long timestamp) {
        public ChatMessage(MsgTypeEnum role, String content) {
            this(role, content, System.currentTimeMillis());
        }
    }

    /**
     * 聊天项实体类，表示一次完整的对话内容
     */
    public record ChatItem(
            String chatId,
            String nodeId,
            List<ChatMessage> userInputs,     // 用户输入
            List<ChatMessage> llmThinking,    // LLM思考过程
            List<ChatMessage> llmResponses    // LLM返回内容
    ) {
        public ChatItem(String chatId, String nodeId) {
            this(chatId, nodeId, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * 创建固定长度的聊天历史记录队列
     */
    private static ConcurrentLinkedQueue<ChatItem> createChatHistoryQueue() {
        return new ConcurrentLinkedQueue<ChatItem>() {
            @Override
            public boolean add(ChatItem item) {
                // 如果队列已满，移除最早的记录
                if (size() >= MAX_HISTORY_LENGTH) {
                    poll();
                }
                return super.add(item);
            }
        };
    }

    public static void newChat(String chatId, String nodeId) {
        String key = generateKey(chatId, nodeId);
        ConcurrentLinkedQueue<ChatItem> queue = chatHistoryCache.getUnchecked(key);
        ChatItem newItem = new ChatItem(chatId, nodeId);
        queue.add(newItem);
    }

    /**
     * 添加聊天记录
     *
     * @param chatId  会话ID
     * @param nodeId  节点ID
     * @param role    消息角色(system/user/assistant)
     * @param content 消息内容
     */
    public static void addMessage(String chatId, String nodeId, MsgTypeEnum role, String content) {
        String key = generateKey(chatId, nodeId);
        ChatMessage message = new ChatMessage(role, content);

        // 获取或创建当前ChatItem
        ConcurrentLinkedQueue<ChatItem> queue = chatHistoryCache.getUnchecked(key);
        ChatItem currentItem = getCurrentOrNewChatItem(queue, chatId, nodeId);

        // 根据角色类型添加消息到相应的列表
        switch (role) {
            case USER:
            case SYSTEM:
                currentItem.userInputs().add(message);
                break;
            case THINKING:
                currentItem.llmThinking().add(message);
                break;
            case ASSISTANT:
                currentItem.llmResponses().add(message);
                break;
        }
    }

    /**
     * 获取当前或新建的ChatItem
     */
    private static ChatItem getCurrentOrNewChatItem(ConcurrentLinkedQueue<ChatItem> queue,
                                                    String chatId, String nodeId) {
        // 获取队列中的最后一个元素
        ChatItem latestItem = null;
        for (ChatItem item : queue) {
            latestItem = item;
        }

        if (latestItem == null) {
            ChatItem newItem = new ChatItem(chatId, nodeId);
            queue.add(newItem);
            return newItem;
        }
        return latestItem;
    }

    /**
     * 获取聊天历史记录
     *
     * @param chatId 会话ID
     * @param nodeId 节点ID
     * @return 聊天历史记录列表
     */
    public static List<ChatItem> getHistory(String chatId, String nodeId) {
        String key = generateKey(chatId, nodeId);
        return new ArrayList<>(chatHistoryCache.getUnchecked(key));
    }

    /**
     * 获取指定条数的聊天历史记录
     *
     * @param chatId 会话ID
     * @param nodeId 节点ID
     * @param count  指定条数
     * @return 指定条数的聊天历史记录列表
     */
    public static List<ChatItem> getHistory(String chatId, String nodeId, int count) {
        String key = generateKey(chatId, nodeId);
        ConcurrentLinkedQueue<ChatItem> queue = chatHistoryCache.getUnchecked(key);

        List<ChatItem> allItems = new ArrayList<>(queue);
        int size = allItems.size();
        int fromIndex = Math.max(0, size - count);

        return new ArrayList<>(allItems.subList(fromIndex, size));
    }

    /**
     * 清除指定会话的聊天历史
     *
     * @param chatId 会话ID
     * @param nodeId 节点ID
     */
    public static void clearHistory(String chatId, String nodeId) {
        String key = generateKey(chatId, nodeId);
        chatHistoryCache.invalidate(key);
    }

    /**
     * 生成缓存key
     *
     * @param chatId 会话ID
     * @param nodeId 节点ID
     * @return 缓存key
     */
    private static String generateKey(String chatId, String nodeId) {
        return chatId + ":" + nodeId;
    }

    /**
     * 获取缓存实例（用于测试或特殊用途）
     */
    public static LoadingCache<String, ConcurrentLinkedQueue<ChatItem>> getCache() {
        return chatHistoryCache;
    }
}