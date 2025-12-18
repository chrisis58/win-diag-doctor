package cn.teacy.wdd.probe;

import cn.teacy.wdd.probe.config.ProbeConfig;
import cn.teacy.wdd.probe.exception.ProbeInitializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.CountDownLatch;


@Slf4j
public class ProbeMainApplication {

    private static final CountDownLatch KEEP_ALIVE_LATCH = new CountDownLatch(1);

    public static void main(String[] args) {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProbeConfig.class)) {

            context.registerShutdownHook();

            // 当 JVM 关闭时，释放 latch
            Runtime.getRuntime().addShutdownHook(new Thread(KEEP_ALIVE_LATCH::countDown));
            KEEP_ALIVE_LATCH.await();

        } catch (ProbeInitializationException e) {
            log.error("探针初始化失败: {}", e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.warn(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("探针启动或运行时发生致命错误: {}", e.getMessage(), e);
            System.exit(1);
        }

    }

}
