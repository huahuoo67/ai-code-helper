package com.fzq.aicodehelper.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisMemoryConfig {

    @Value("${app.redis.host:127.0.0.1}")
    private String host;

    @Value("${app.redis.port:6379}")
    private int port;

    @Value("${app.redis.user:default}")
    private String user;

    @Value("${app.redis.password:}")
    private String password;

    @Value("${app.redis.ttl-seconds:604800}")
    private long ttlSeconds;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        var builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .prefix("ai-code-helper:chat-memory:")
                .ttl(ttlSeconds)
                .storeType(StoreType.STRING);

        if (password != null && !password.isBlank()) {
            builder.user(user)
                    .password(password);
        }
        return builder.build();
    }
}
