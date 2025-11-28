package cn.teacy.wdd.probe.utils;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HostnameUtil {

    public static String getHostname() {
        String hostname = System.getenv("COMPUTERNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }

        hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Failed to resolve hostname via InetAddress", e);
        }

        return "Unknown-Host-" + System.currentTimeMillis();
    }

}
