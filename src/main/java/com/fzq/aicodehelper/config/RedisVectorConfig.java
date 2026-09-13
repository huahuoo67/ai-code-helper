package com.fzq.aicodehelper.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;

@Configuration
public class RedisVectorConfig {

    @Value("${app.redis.host:127.0.0.1}")
    private String host;

    @Value("${app.redis.port:6379}")
    private int port;

    @Value("${app.redis.user:default}")
    private String user;

    @Value("${app.redis.password:}")
    private String password;

    @Value("${app.redis.vector.index-name:ai-code-helper-vector-index}")
    private String indexName;

    @Value("${app.redis.vector.key-prefix:ai-code-helper:vector:}")
    private String keyPrefix;

    @Value("${app.redis.vector.dimension:1024}")
    private int dimension;

    @Bean(destroyMethod = "close")
    public UnifiedJedis redisVectorJedis() {
        var configBuilder = DefaultJedisClientConfig.builder();

        if (password != null && !password.isBlank()) {
            configBuilder.user(user)
                    .password(password);
        }

        return new UnifiedJedis(
                new HostAndPort(host, port),
                configBuilder.build()
        );
    }

    @Bean(name = "redisEmbeddingStore")
    public EmbeddingStore<TextSegment> redisEmbeddingStore(
            UnifiedJedis redisVectorJedis
    ) {
        return RedisEmbeddingStore.builder()
                .unifiedJedis(redisVectorJedis)
                .indexName(indexName)
                .prefix(keyPrefix)
                .dimension(dimension)
                .build();
    }
}