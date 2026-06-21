package com.tianji.user.service.adminops;

import org.springframework.util.StringUtils;

public final class AdminOpsNetworkMetricParser {

    private AdminOpsNetworkMetricParser() {
    }

    public static NetworkUsageSnapshot parseProcNetDev(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new NetworkUsageSnapshot(0L, 0L);
        }
        long nonLoopbackRx = 0L;
        long nonLoopbackTx = 0L;
        long loopbackRx = 0L;
        long loopbackTx = 0L;
        for (String line : raw.split("\\r?\\n")) {
            if (!line.contains(":")) {
                continue;
            }
            String[] ifaceParts = line.split(":", 2);
            if (ifaceParts.length != 2) {
                continue;
            }
            String iface = ifaceParts[0].trim();
            String[] metrics = ifaceParts[1].trim().split("\\s+");
            if (metrics.length < 9) {
                continue;
            }
            long received = parseLong(metrics[0]);
            long sent = parseLong(metrics[8]);
            if ("lo".equals(iface)) {
                loopbackRx += received;
                loopbackTx += sent;
            } else {
                nonLoopbackRx += received;
                nonLoopbackTx += sent;
            }
        }
        if (nonLoopbackRx > 0L || nonLoopbackTx > 0L) {
            return new NetworkUsageSnapshot(nonLoopbackRx, nonLoopbackTx);
        }
        return new NetworkUsageSnapshot(loopbackRx, loopbackTx);
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
