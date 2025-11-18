package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.common.constants.LogNames;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.probe.config.ProbeConfig;
import cn.teacy.wdd.probe.reader.IWinEventLogCleaner;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.shipper.IProbeShipper;
import cn.teacy.wdd.protocol.WsMessageContext;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

@Slf4j
public class ProbeMainApplication {

    public static void main(String[] args) {

        try {
            ApplicationContext context = new AnnotationConfigApplicationContext(ProbeConfig.class);

            IWinEventLogReader reader = context.getBean(IWinEventLogReader.class);
            IProbeShipper shipper = context.getBean(IProbeShipper.class);
            IWinEventLogCleaner cleaner = context.getBean(IWinEventLogCleaner.class);

            LogQueryRequest queryRequest = LogQueryRequest.builder()
                    .logName(LogNames.SYSTEM)
                    .levels(List.of(LogLevel.CRITICAL, LogLevel.ERROR))
                    .maxEvents(3)
                    .build();

            List<WinEventLogEntry> logEntries = reader.readEventLogs(queryRequest);
            log.debug("读取到的日志条目: {}", logEntries);

            List<WinEventLogEntry> handled = cleaner.handle(logEntries);
            log.debug("清理后的日志条目: {}", handled);

            boolean ret = shipper.ship("test-task-id", new WsMessageContext(queryRequest, handled));
            log.info("日志发送结果: {}", ret ? "成功" : "失败");

        } catch (Exception e) {
            log.error("探针启动或运行时发生致命错误: {}", e.getMessage(), e);
        }

    }

}
