package org.github.siahsang.redutils.redis;

public class RedisAddress {
    public final int masterPort;

    public final String masterHostAddress;

    public RedisAddress(int masterPort, String masterHostAddress) {
        this.masterPort = masterPort;
        this.masterHostAddress = masterHostAddress;
    }

}
