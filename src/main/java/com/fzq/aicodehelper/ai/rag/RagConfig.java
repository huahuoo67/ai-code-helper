package com.fzq.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.UnifiedJedis;

import java.util.List;


/**
 * 加载 RAG
 * 实现文档检索
 * 下面这只是一个标准版的示例。
 * 还有更复杂的详细的流水线rag
 */
@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource(name = "redisEmbeddingStore")
    private EmbeddingStore<TextSegment>  embeddingStore;

    @Resource(name = "redisVectorJedis")
    private UnifiedJedis redisVectorJedis;

    @Value("${app.redis.vector.ingestion-marker:ai-code-helper:rag:indexed:v1}")
    private String ingestionMarker;

    @Bean
    public ContentRetriever contentRetriever() {
        //-----RAG------
        //1.加载文档
        List<Document> documents = ClassPathDocumentLoader.loadDocumentsRecursively("docs");
        //2.文档切割：每个文档按照段落进行分割，最大200个字符，每次最多重叠40字符
        DocumentByParagraphSplitter documentByParagraphSplitter =
                new DocumentByParagraphSplitter(200,40);
        //3.构建知识库索引：将文档切片、向量化，并保存到向量库。
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByParagraphSplitter)
                // 为了提高文档的质量，为每个切割后的文档碎片 TextSegment 添加文档名称作为元信息。将文件名拼接到文本片段前，使文件名也参与向量化
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name")
                        + "\n" + textSegment.text(),textSegment.metadata()))
                //使用的向量模型
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        //加载文档
        // ingestor.ingest(documents);  这行代码只管加载，实际运行之后没启动一次都会重复加载一次
        if (!redisVectorJedis.exists(ingestionMarker)) {
            ingestor.ingest(documents);

            // 向量全部写入成功之后再设置标记
            redisVectorJedis.set(ingestionMarker, "completed");
        }
        //4.自定义内容加载器
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5)// 最多5条结果
                .minScore(0.75)// 过滤掉分数小于 0.75 的结果
                .build();
        return contentRetriever;
    }

}
