package com.fzq.aicodehelper.ai;

import com.fzq.aicodehelper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel qwenchatModel;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    @Resource
    private InterviewQuestionTool interviewQuestionTool;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Bean
    public AiCodeHelperService aiCodeHelperService(StreamingChatModel streamingChatModel) {
        //回话记忆窗口设置,备注是调的是单个的模型回话记忆机制。下面的记忆提供者是根据id来创建一个不同的会话。二选一即可
        // ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        //构造aiservice
        return AiServices.builder(AiCodeHelperService.class)
                .chatModel(qwenchatModel)
                .streamingChatModel(qwenStreamingChatModel) // 流式对话输出
                // .chatMemory(ChatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build()) // 每个回话独立存储
                .contentRetriever(contentRetriever)// 文本检索rag
                .tools(interviewQuestionTool)// 工具调用
                .toolProvider(mcpToolProvider)// MCP工具调用
                .build();
    }

}
