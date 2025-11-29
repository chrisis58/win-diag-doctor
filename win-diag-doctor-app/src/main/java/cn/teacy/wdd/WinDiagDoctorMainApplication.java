package cn.teacy.wdd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
        "com.alibaba.cloud.ai.agent.studio.SaaStudioWebModuleAutoConfiguration"
})
public class WinDiagDoctorMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(WinDiagDoctorMainApplication.class, args);
    }

}
