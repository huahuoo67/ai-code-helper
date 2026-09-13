package com.fzq.aicodehelper.ai;

import com.fzq.aicodehelper.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

import java.util.List;

@InputGuardrails(SafeInputGuardrail.class)
public interface AiCodeHelperService {

    record Report(String name , List<String> list){}

    // 这里就是直接将ai输出的返回即可，就是字符串类型
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String message);

    // 自定义一个返回的结构体，ai在返回的时候发现是这个样的结果，就会在自动填充，相当于是告诉ai将结果返回成json的形式
    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String message);

    // 这里设置这个结果集，返回
    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    String chatWithTools(String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String message);

}
