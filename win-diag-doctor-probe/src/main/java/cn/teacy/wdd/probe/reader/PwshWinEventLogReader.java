package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.common.dto.LogQueryRequest;
import cn.teacy.wdd.common.dto.WinEventLogEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用 PowerShell 读取 Windows 事件日志的实现
 */
@Slf4j
public class PwshWinEventLogReader implements IWinEventLogReader {

    private static final int TIMEOUT_SECONDS = 30;

    private final ObjectMapper objectMapper;

    public PwshWinEventLogReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<WinEventLogEntry> readEventLogs(LogQueryRequest queryRequest) {

        String powerShellCommand = buildPowerShellCommand(
                queryRequest
        );

        String[] command = {
                "cmd.exe",
                "/C",
                // "chcp 65001 >NUL" 切换代码页并抑制 "Active code page: 65001" 这条消息
                // "&&" 确保只有 chcp 成功后才执行 powershell
                // -NoProfile 启动 PowerShell 更快
                "chcp 65001 >NUL && powershell.exe -NoProfile -Command \"" + powerShellCommand + "\""
        };

        StringBuilder jsonOutput = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(command);

            Charset consoleEncoding = StandardCharsets.UTF_8;

            // 捕获标准输出流
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), consoleEncoding))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonOutput.append(line);
                }
            }

            // 捕获错误流
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), consoleEncoding))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }
            }

            // 等待进程结束，设置一个超时
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy();
                log.error("PowerShell 命令执行超时！");
                return new ArrayList<>();
            }

            // 检查是否有错误
            if (process.exitValue() != 0) {
                log.error("PowerShell 命令执行失败，退出码: {}", process.exitValue());
                log.error("错误详情: {}", errorOutput);
                return new ArrayList<>();
            }

            String json = jsonOutput.toString();
            if (json.isEmpty()) {
                log.info("未找到匹配的日志条目。");
                return new ArrayList<>();
            }

            // PowerShell 在返回单个对象时也会将其包裹在数组中，所以这里始终解析为 List
            return objectMapper.readValue(json, new TypeReference<List<WinEventLogEntry>>() {});

        } catch (Exception e) {
            log.error("读取 Windows 事件日志时发生异常", e);
            log.error(e.getMessage(), e);

            return new ArrayList<>();
        }
    }

    private String buildPowerShellCommand(LogQueryRequest queryRequest) {
        String levelString = String.join(",",
                queryRequest.getLevels().stream().map(LogLevel::getValue).map(String::valueOf).toArray(String[]::new)
        );

        return String.format(
                "$OutputEncoding = [System.Text.Encoding]::UTF8; Get-WinEvent -FilterHashtable @{LogName='%s'; Level=%s} -MaxEvents %d | ConvertTo-Json",
                queryRequest.getLogName(), levelString, queryRequest.getMaxEvents()
        );
    }

}
