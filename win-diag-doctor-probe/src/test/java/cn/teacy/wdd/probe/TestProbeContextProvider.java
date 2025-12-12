package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.probe.component.PowerShellExecutor;
import cn.teacy.wdd.probe.component.ProbeContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestProbeContextProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PowerShellExecutor powerShellExecutor = new PowerShellExecutor();

    private ProbeContextProvider probeContextProvider;

    @BeforeEach
    void setUp() {
        probeContextProvider = new ProbeContextProvider(powerShellExecutor, objectMapper);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testGetProbeContext() {
        UserContext userContext = probeContextProvider.getUserContext();

        assertNotNull(userContext);

        System.out.println(userContext);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCaching() {
        UserContext firstCall = probeContextProvider.getUserContext();
        UserContext secondCall = probeContextProvider.getUserContext();

        assertNotNull(firstCall);
        assertNotNull(secondCall);

        assert(firstCall == secondCall);
    }

}
