package com.tianji.user.service.adminops;

public class NetworkUsageSnapshot {

    private final long receivedBytes;
    private final long sentBytes;

    public NetworkUsageSnapshot(long receivedBytes, long sentBytes) {
        this.receivedBytes = Math.max(receivedBytes, 0L);
        this.sentBytes = Math.max(sentBytes, 0L);
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public long getSentBytes() {
        return sentBytes;
    }

    public boolean hasTraffic() {
        return receivedBytes > 0L || sentBytes > 0L;
    }
}
