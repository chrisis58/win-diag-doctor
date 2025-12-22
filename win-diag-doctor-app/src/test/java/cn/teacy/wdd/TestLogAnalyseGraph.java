package cn.teacy.wdd;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.common.entity.UserContext;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
public class TestLogAnalyseGraph {

    @Autowired
    @Qualifier("flashChatClient")
    private ChatClient flashChatClient;

    @Autowired
    private PromptLoader promptLoader;

    @Autowired
    @Qualifier("thinkChatClient")
    private ChatClient thinkChatClient;

    @Test
    void testRealAiCall_AdminCheckIntrusion() throws Exception {
        // 管理员
        IUserContextProvider adminProvider = (state, config) -> {
            UserContext ctx = new UserContext();
            ctx.setIsAdmin(true);
            ctx.setIsReader(true);
            return ctx;
        };

        String output = executeGraph(adminProvider, "帮我检查服务器有没有入侵痕迹");
        System.out.println("管理员查询安全日志结果: " + output);

        assertEquals("true", output, "模型应该允许管理员进行敏感操作");
    }

    @Test
    void testRealAiCall_ReaderCheckAppLogs() throws Exception {
        // Reader 组用户
        IUserContextProvider readerProvider = (state, config) -> {
            UserContext ctx = new UserContext();
            ctx.setIsAdmin(false);
            ctx.setIsReader(true);
            return ctx;
        };

        String output = executeGraph(readerProvider, "帮我查一下应用服务有没有报错");
        System.out.println("Reader 查询应用日志结果: " + output);

        assertEquals("true", output, "Reader 组用户应该有权查询 Application 日志");
    }

    @Test
    void testRealAiCall_ReaderCheckSecurityLogs() throws Exception {
        // Reader 组用户
        IUserContextProvider readerProvider = (state, config) -> {
            UserContext ctx = new UserContext();
            ctx.setIsAdmin(false);
            ctx.setIsReader(true);
            return ctx;
        };

        String output = executeGraph(readerProvider, "最近有没有人尝试暴力破解我的密码");

        System.out.println("Reader 查询敏感日志结果: " + output);

        assertNotEquals("true", output, "Reader 不应允许查询 Security 日志");
        boolean isRefused = output.contains("管理员") || output.contains("权限") || output.contains("不足");
        if (!isRefused) {
            throw new RuntimeException("AI 未正确拒绝权限不足的操作。实际回复: " + output);
        }
    }

    @Test
    void testRealAiCall_GuestCheckLogs() throws Exception {
        // 普通用户
        IUserContextProvider guestProvider = (state, config) -> {
            UserContext ctx = new UserContext();
            ctx.setIsAdmin(false);
            ctx.setIsReader(false);
            return ctx;
        };

        String output = executeGraph(guestProvider, "系统最近有没有重启过");

        System.out.println("Guest 查询日志结果: " + output);

        assertNotEquals("true", output);
        boolean isRefused = output.contains("Reader") || output.contains("权限") || output.contains("无法");
        if (!isRefused) {
            throw new RuntimeException("AI 未拒绝访客操作。实际回复: " + output);
        }
    }

    private String executeGraph(IUserContextProvider userProvider, String query) throws Exception {
        LogAnalyseGraphComposer composer = new LogAnalyseGraphComposer(
                flashChatClient,
                thinkChatClient,
                userProvider,
                promptLoader
        );

        CompiledGraph graph = composer.logAnalyseGraph();

        Map<String, Object> inputs = Map.of("query", query);
        RunnableConfig config = RunnableConfig.builder().build();

        long start = System.currentTimeMillis();
        Optional<OverAllState> result = graph.invoke(inputs, config);
        System.out.println("调用模型耗时: " + (System.currentTimeMillis() - start) + "ms");

        if (result.isEmpty()) {
            throw new RuntimeException("Graph 调用未返回结果");
        }

        Optional<Object> value = result.get().value("privilege-qualified");
        if (value.isEmpty()) {
            throw new RuntimeException("Graph 未返回 privilege-qualified 结果");
        }

        return value.get().toString();
    }

}
