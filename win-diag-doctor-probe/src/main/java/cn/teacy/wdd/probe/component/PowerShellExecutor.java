package cn.teacy.wdd.probe.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PowerShellExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 执行 PowerShell 命令并返回标准输出内容
     *
     * @param psCommand 纯 PowerShell 命令脚本
     * @return 标准输出 (Stdout) 的内容
     * @throws IOException           IO异常
     * @throws InterruptedException  线程中断
     * @throws IllegalStateException 如果脚本执行出错（ExitCode != 0）
     */
    public String execute(String psCommand) throws IOException, InterruptedException {
        return execute(psCommand, DEFAULT_TIMEOUT_SECONDS);
    }

    public String execute(String psCommand, int timeoutSeconds) throws IOException, InterruptedException {
        psCommand = Base64.getEncoder().encodeToString(
                psCommand.getBytes(StandardCharsets.UTF_16LE)
        );

        String[] command = {
                "cmd.exe",
                "/C",
                // chcp 65001 切换 UTF-8，&& 确保切换成功再执行
                "chcp 65001 >NUL && powershell.exe -NoProfile -NonInteractive -EncodedCommand \"" + psCommand + "\""
        };

        ProcessBuilder pb = new ProcessBuilder(command);

        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try (BufferedReader stdReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = stdReader.readLine()) != null) {
                stdout.append(line);
            }

            while ((line = errReader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
        }

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("PowerShell execution timed out after " + timeoutSeconds + " seconds.");
        }

        if (process.exitValue() != 0) {
            String errorMsg = stderr.toString().trim();
            if (errorMsg.contains("NoMatchingEventsFound") || errorMsg.contains("No events were found")) {
                log.info("PowerShell查询未返回结果 (No events found)");
                return "";
            }

            log.error("PowerShell exited with code {}. Error: {}", process.exitValue(), errorMsg);
            throw new IllegalStateException("PowerShell command failed: " + errorMsg);
        }

        return stdout.toString().trim();
    }
}
