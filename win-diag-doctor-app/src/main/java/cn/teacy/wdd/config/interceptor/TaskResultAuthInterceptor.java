package cn.teacy.wdd.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

import static cn.teacy.wdd.common.constants.ProbeConstants.PROBE_ID_HEADER;

@Slf4j
public class TaskResultAuthInterceptor implements HandlerInterceptor {

    @Value("${wdd.probe.connect-key}")
    private String probeConnectKey;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {

        String probeId = request.getHeader(PROBE_ID_HEADER);

        String authHeader = request.getHeader("Authorization");

        log.debug("Authenticating task result callback: probeId={}, Authorization={}", probeId, authHeader);

        if (Objects.nonNull(authHeader) && authHeader.equals("Bearer " + DigestUtils.md5Hex(probeConnectKey + probeId))) {
            return true;
        } else  {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("Unauthorized access attempt for probeId={}", probeId);
            return false;
        }
    }

}
