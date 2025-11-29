package cn.teacy.wdd.support;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
@Component
@RequestScope
public class ProbeContext {

    private String probeId;

}
