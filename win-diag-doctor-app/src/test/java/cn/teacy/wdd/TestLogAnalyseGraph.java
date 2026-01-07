package cn.teacy.wdd;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.service.IEventLogQueryService;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.agent.utils.GraphUtils;
import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
public class TestLogAnalyseGraph {

    @MockitoBean
    private IEventLogQueryService mockEventLogQueryService;

    @MockitoBean
    private IUserContextProvider mockUserContextProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("logAnalyseGraph")
    private CompiledGraph logAnalyseGraph;

    private static LogQueryResponse CACHED_RESPONSE;

    @BeforeEach
    void setupMock() throws Exception {
        if (CACHED_RESPONSE == null) {
            CACHED_RESPONSE = objectMapper.readValue(TEST_SYSTEM_LEVEL_LOG, LogQueryResponse.class);
        }

        BDDMockito.given(mockEventLogQueryService.queryEventLogs(any(), any()))
                .willReturn(CACHED_RESPONSE);
    }

    @Test
    void testRealAiCall_AdminCheckIntrusion() throws Exception {
        UserContext ctx = new UserContext(true, true);

        BDDMockito.given(mockUserContextProvider.getUserContext(any(), any()))
                .willAnswer(invocation -> ctx);

        String output = executeGraph("帮我检查服务器有没有入侵痕迹");
        System.out.println("管理员查询安全日志结果: " + output);

        assertEquals("true", output, "模型应该允许管理员进行敏感操作");
    }

    @Test
    void testRealAiCall_ReaderCheckAppLogs() throws Exception {
        // Reader 组用户
        UserContext ctx = new UserContext(false, true);
        BDDMockito.given(mockUserContextProvider.getUserContext(any(), any()))
                .willAnswer(invocation -> ctx);

        String output = executeGraph("帮我查一下应用服务有没有报错");
        System.out.println("Reader 查询应用日志结果: " + output);

        assertEquals("true", output, "Reader 组用户应该有权查询 Application 日志");
    }

    @Test
    void testRealAiCall_ReaderCheckSecurityLogs() throws Exception {
        // Reader 组用户
        UserContext ctx = new UserContext(false, true);
        BDDMockito.given(mockUserContextProvider.getUserContext(any(), any()))
                .willAnswer(invocation -> ctx);

        String output = executeGraph("最近有没有人尝试暴力破解我的密码");

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
        UserContext ctx = new UserContext(false, false);
        BDDMockito.given(mockUserContextProvider.getUserContext(any(), any()))
                .willAnswer(invocation -> ctx);

        String output = executeGraph("系统最近有没有重启过");

        System.out.println("Guest 查询日志结果: " + output);

        assertNotEquals("true", output);
        boolean isRefused = output.contains("Reader") || output.contains("权限") || output.contains("无法");
        if (!isRefused) {
            throw new RuntimeException("AI 未拒绝访客操作。实际回复: " + output);
        }
    }

    @Test
    void testRealAiCall_InterruptedGraph() throws Exception {
        // 普通用户
        UserContext ctx = new UserContext(false, false);
        BDDMockito.given(mockUserContextProvider.getUserContext(any(), any()))
                .willAnswer(invocation -> ctx);

        Map<String, Object> inputs = Map.of(LogAnalyseGraphComposer.KEY_QUERY, "帮我检查系统日志有没有异常");
        RunnableConfig config = RunnableConfig.builder().build();

        long start = System.currentTimeMillis();
        GraphUtils.GraphExecResult graphExecResult = GraphUtils.executeUntilInterrupt(logAnalyseGraph, inputs, config);
        System.out.println("调用模型耗时: " + (System.currentTimeMillis() - start) + "ms");

        assertTrue(graphExecResult.state().value(LogAnalyseGraphComposer.KEY_EXECUTION_PLAN).isEmpty());
    }

    @Test
    void testRealAiCall_GraphExecuteWithoutInterruption() throws Exception {
        // 管理员
        UserContext ctx = new UserContext(true, true);
        BDDMockito.given(mockUserContextProvider.getUserContext(any(), any()))
                .willAnswer(invocation -> ctx);

        Map<String, Object> inputs = Map.of("query", "帮我检查系统日志有没有异常");
        RunnableConfig config = RunnableConfig.builder().build();

        long start = System.currentTimeMillis();
        GraphUtils.GraphExecResult graphExecResult = GraphUtils.executeUntilInterrupt(logAnalyseGraph, inputs, config);
        System.out.println("调用模型耗时: " + (System.currentTimeMillis() - start) + "ms");

        assertFalse(graphExecResult.interrupted(), "Graph 不应该被中断");
        assertTrue(graphExecResult.state().value(LogAnalyseGraphComposer.KEY_EXECUTION_PLAN).isPresent());
    }

    private String executeGraph(String query) throws Exception {

        Map<String, Object> inputs = Map.of("query", query);
        RunnableConfig config = RunnableConfig.builder().build();

        long start = System.currentTimeMillis();
        Optional<OverAllState> result = logAnalyseGraph.invoke(inputs, config);
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

    private static final String TEST_SYSTEM_LEVEL_LOG = """
            {
                "userContext": {
                    "isAdmin": false,
                    "isReader": true
                },
                "hasMore": true,
                "entries": [
                    {
                        "Id": 1801,
                        "LevelDisplayName": "Error",
                        "ProviderName": "Microsoft-Windows-TPM-WMI",
                        "TimeCreated": "2025-12-25 09:51:36",
                        "Message": "Secure Boot CA/keys need to be updated. This device signature information is included here.\\r\\nDeviceAttributes: FirmwareVersion:[REDACTED];OEMManufacturerName:[OEM_NAME];OEMModelSKU:[OEM_MODEL_INFO];OSArchitecture:amd64;\\r\\nBucketId: [REDACTED_BUCKET_ID]\\r\\nBucketConfidenceLevel: \\r\\nUpdateType: 0\\r\\nHResult: The operation completed successfully."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:49:32",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 激活 permission for the COM Server application with CLSID \\r\\n{2593F8B9-4EAF-457C-B68A-50F6B8EA6B54}\\r\\n and APPID \\r\\n{15C20B67-12E7-4BB6-92BB-7AFF07997402}\\r\\n to the user [DOMAIN]\\\\[USER] SID (S-1-5-21-[REDACTED]-1001) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:49:29",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 启动 permission for the COM Server application with CLSID \\r\\nWindows.SecurityCenter.SecurityAppBroker\\r\\n and APPID \\r\\n不可用\\r\\n to the user NT AUTHORITY\\\\SYSTEM SID (S-1-5-18) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:49:29",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 启动 permission for the COM Server application with CLSID \\r\\nWindows.SecurityCenter.WscBrokerManager\\r\\n and APPID \\r\\n不可用\\r\\n to the user NT AUTHORITY\\\\SYSTEM SID (S-1-5-18) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    },
                    {
                        "Id": 7000,
                        "LevelDisplayName": "Error",
                        "ProviderName": "Service Control Manager",
                        "TimeCreated": "2025-12-25 09:49:03",
                        "Message": "The Google 更新服务 (gupdate) service failed to start due to the following error: \\r\\nThe service did not respond to the start or control request in a timely fashion."
                    },
                    {
                        "Id": 7009,
                        "LevelDisplayName": "Error",
                        "ProviderName": "Service Control Manager",
                        "TimeCreated": "2025-12-25 09:49:03",
                        "Message": "A timeout was reached (120000 milliseconds) while waiting for the Google 更新服务 (gupdate) service to connect."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:48:56",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 激活 permission for the COM Server application with CLSID \\r\\n{2593F8B9-4EAF-457C-B68A-50F6B8EA6B54}\\r\\n and APPID \\r\\n{15C20B67-12E7-4BB6-92BB-7AFF07997402}\\r\\n to the user [DOMAIN]\\\\[USER] SID (S-1-5-21-[REDACTED]-1001) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:48:30",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 启动 permission for the COM Server application with CLSID \\r\\nWindows.SecurityCenter.WscCloudBackupProvider\\r\\n and APPID \\r\\n不可用\\r\\n to the user [DOMAIN]\\\\[USER] SID (S-1-5-21-[REDACTED]-1001) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:48:21",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 激活 permission for the COM Server application with CLSID \\r\\n{2593F8B9-4EAF-457C-B68A-50F6B8EA6B54}\\r\\n and APPID \\r\\n{15C20B67-12E7-4BB6-92BB-7AFF07997402}\\r\\n to the user [DOMAIN]\\\\[USER] SID (S-1-5-21-[REDACTED]-1001) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    },
                    {
                        "Id": 10016,
                        "LevelDisplayName": "Warning",
                        "ProviderName": "Microsoft-Windows-DistributedCOM",
                        "TimeCreated": "2025-12-25 09:48:20",
                        "Message": "The 应用程序-特定 permission settings do not grant 本地 启动 permission for the COM Server application with CLSID \\r\\nWindows.SecurityCenter.WscCloudBackupProvider\\r\\n and APPID \\r\\n不可用\\r\\n to the user [DOMAIN]\\\\[USER] SID (S-1-5-21-[REDACTED]-1001) from address LocalHost (使用 LRPC) running in the application container 不可用 SID (不可用). This security permission can be modified using the Component Services administrative tool."
                    }
                ]
            }
            """;

}
