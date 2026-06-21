package com.tianji.user.service.adminops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOpsNetworkMetricParserTest {

    @Test
    void parseProcNetDev_shouldSumAllNonLoopbackInterfaces() {
        String raw = ""
                + "Inter-|   Receive                                                |  Transmit\n"
                + " face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n"
                + "    lo: 1000 10 0 0 0 0 0 0 2000 20 0 0 0 0 0 0\n"
                + "  eth0: 4096 40 0 0 0 0 0 0 8192 80 0 0 0 0 0 0\n"
                + "  eth1: 2048 20 0 0 0 0 0 0 1024 10 0 0 0 0 0 0\n";

        NetworkUsageSnapshot snapshot = AdminOpsNetworkMetricParser.parseProcNetDev(raw);

        assertEquals(6144L, snapshot.getReceivedBytes());
        assertEquals(9216L, snapshot.getSentBytes());
    }

    @Test
    void parseProcNetDev_shouldFallbackToLoopbackWhenNoOtherInterfaceExists() {
        String raw = ""
                + "Inter-|   Receive                                                |  Transmit\n"
                + " face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n"
                + "    lo: 512 5 0 0 0 0 0 0 256 2 0 0 0 0 0 0\n";

        NetworkUsageSnapshot snapshot = AdminOpsNetworkMetricParser.parseProcNetDev(raw);

        assertEquals(512L, snapshot.getReceivedBytes());
        assertEquals(256L, snapshot.getSentBytes());
    }
}
