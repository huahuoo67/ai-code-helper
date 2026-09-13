package com.fzq.aicodehelper.ai;

import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void chat() {
        String result = aiCodeHelperService.chat("你好，我是花火！");
        System.out.println(result);
    }
    @Test
    void chatWithMemory() {
        String result1 = aiCodeHelperService.chat("你好，我是花火！");
        System.out.println(result1);
        String result2 = aiCodeHelperService.chat("我是谁？");
        System.out.println(result2);
    }

    @Test
    void chatForReport() {
        String messxage = ("你好，我是花火");
        AiCodeHelperService.Report report = aiCodeHelperService.chatForReport(messxage);
        System.out.println(report);

    }

    // 这是正常写的返回string的结果
    /* @Test
    void chatWithRag() {
        String result = aiCodeHelperService.chat("怎么学习java，有哪些常见的面试题？");
        System.out.println(result);
    } */

    // 这是用同一的result进行接收返回值的形式。可以获取一些别的参数，比如回答参考的源文档信息，tokens使用量信息等
    @Test
    void chatWithRag() throws NoSuchMethodException {
        Result<String> result = aiCodeHelperService.chatWithRag("怎么学习java，有哪些常见的面试题？");
        // 这里不是因为已经变成了一个封装对象，所以就是现在这样打印结果。
        // 下面的打印结果：dev.langchain4j.service.Result@2c7375da  所以加不加tostring都一样
        System.out.println(result.toString());
        System.out.println("+++++++++++++");
        System.out.println(result.content());
        System.out.println("+++++++++++++");
        System.out.println(result.sources());
        System.out.println("+++++++++++++");
        // 下面的打印结果：class java.lang.Object
        // 所以就是继承了object的tostring方法，但是并没有实现
        System.out.println(
                result.getClass()
                        .getMethod("toString")
                        .getDeclaringClass()
        );
    }

    @Test
    void chatWithTools() {
        String result = aiCodeHelperService.chatWithTools("请搜索计算机网络相关面试题");
        System.out.println(result);
    }
    @Test
    void chatWithMcp() {
        String result = aiCodeHelperService.chat("请搜索有关于编程导航的信息");
        System.out.println(result);
    }
    @Test
    void chatWithGuardrail() {
        String result = aiCodeHelperService.chat("kill the game");
        System.out.println(result);
    }
}