package cn.teacy.wdd.probe.component;

import cn.teacy.wdd.common.entity.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProbeContextProvider {

    private static final String CHECK_PERMISSION_SCRIPT = """
            $currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent());
            $isAdmin = $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator);

            $isReader = $currentPrincipal.IsInRole([System.Security.Principal.SecurityIdentifier]'S-1-5-32-573');

            @{ isAdmin = $isAdmin; isReader = $isReader } | ConvertTo-Json -Compress
            """;

    private static final UserContext UNKNOWN_CONTEXT = new UserContext(false, false);

    private volatile UserContext userContext;

    private final PowerShellExecutor powerShellExecutor;
    private final ObjectMapper objectMapper;


    public UserContext getUserContext() {
        if (userContext == null) {
            synchronized (this) {
                if (userContext == null) {
                    try {
                        String result = powerShellExecutor.execute(CHECK_PERMISSION_SCRIPT);

                        if (result == null || result.isEmpty()) {
                            log.warn("Permission check returned empty result.");
                            userContext = UNKNOWN_CONTEXT;
                        } else {
                            userContext = objectMapper.readValue(result, UserContext.class);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        return UNKNOWN_CONTEXT;
                    }
                }
            }
        }
        return userContext;
    }

}
