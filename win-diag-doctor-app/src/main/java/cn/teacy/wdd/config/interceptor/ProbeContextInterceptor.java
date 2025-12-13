package cn.teacy.wdd.config.interceptor;

import cn.teacy.wdd.support.ProbeContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import static cn.teacy.wdd.common.constants.ProbeConstants.PROBE_ID_HEADER;

@Slf4j
@RequiredArgsConstructor
public class ProbeContextInterceptor implements HandlerInterceptor {

    private final ProbeContext probeContext;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {

        // 尝试从请求中提取 Probe ID
        String probeId = request.getParameter("probeId");

        // 尝试从 Header 获取
        if (probeId == null || probeId.isBlank()) {
            probeId = request.getHeader(PROBE_ID_HEADER);
        }

        // 从 Referer 获取
        if (probeId == null || probeId.isBlank()) {
            probeId = extractProbeIdFromReferer(request);
        }

        if (probeId != null && !probeId.isBlank()) {
            log.debug("从中提取到 Probe ID: {}", probeId);
            probeContext.setProbeId(probeId);
        } else {
            log.warn("无法从请求中提取 Probe ID");
            // redirect to dashboard
            response.sendRedirect("/dashboard.html");
            return false;
        }

        return true;
    }

    private String extractProbeIdFromReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        // Referer: http://localhost:8093/chatui/index.html?probeId=ababab

        if (referer != null && !referer.isBlank()) {
            try {
                return UriComponentsBuilder.fromUriString(referer)
                        .build()
                        .getQueryParams()
                        .getFirst("probeId");
            } catch (Exception e) {
                log.warn("无法从 Referer 解析 Probe ID: {}", referer);
            }
        }
        return null;
    }

}
