package org.github.siahsang.redutils.common;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public class RedUtilsConfig {
    public static final String DEFAULT_HOST_ADDRESS = "127.0.0.1";

    public static final int DEFAULT_PORT = 6379;

    private final int waitingTimeForReplicasMillis;

    private final int retryCountForSyncingWithReplicas;

    private final int leaseTimeMillis;

    private final int readTimeOutMillis;

    private final int lockMaxPoolSize;

    private final int channelMaxPoolSize;

    private final String redUtilsUnLockedMessagePattern;

    private final int replicaCount;

    private final String hostAddress;

    public final int port;

    private RedUtilsConfig(RedUtilsConfigBuilder redUtilsConfigBuilder) {
        this.waitingTimeForReplicasMillis = redUtilsConfigBuilder.waitingTimeForReplicasMillis;
        this.retryCountForSyncingWithReplicas = redUtilsConfigBuilder.retryCountForSyncingWithReplicas;
        this.leaseTimeMillis = redUtilsConfigBuilder.leaseTimeMillis;
        this.readTimeOutMillis = redUtilsConfigBuilder.readTimeOutMillis;
        this.lockMaxPoolSize = redUtilsConfigBuilder.lockMaxPoolSize;
        this.channelMaxPoolSize = redUtilsConfigBuilder.channelMaxPoolSize;
        this.redUtilsUnLockedMessagePattern = redUtilsConfigBuilder.redUtilsUnLockedMessage;
        this.replicaCount = redUtilsConfigBuilder.replicaCount;
        this.hostAddress = redUtilsConfigBuilder.hostAddress;
        this.port = redUtilsConfigBuilder.port;

    }

    public int getWaitingTimeForReplicasMillis() {
        return waitingTimeForReplicasMillis;
    }

    public int getRetryCountForSyncingWithReplicas() {
        return retryCountForSyncingWithReplicas;
    }

    public int getLeaseTimeMillis() {
        return leaseTimeMillis;
    }

    public int getReadTimeOutMillis() {
        return readTimeOutMillis;
    }

    public int getLockMaxPoolSize() {
        return lockMaxPoolSize;
    }

    public int getChannelMaxPoolSize() {
        return channelMaxPoolSize;
    }

    public String getRedUtilsUnLockedMessagePattern() {
        return redUtilsUnLockedMessagePattern;
    }

    public int getReplicaCount() {
        return replicaCount;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public int getPort() {
        return port;
    }


    public static final class RedUtilsConfigBuilder {
        private int waitingTimeForReplicasMillis = 1000;

        private int retryCountForSyncingWithReplicas = 3;

        private int leaseTimeMillis = 30_000;

        private int readTimeOutMillis = 2000;

        private int lockMaxPoolSize = 60;

        private int channelMaxPoolSize = 51;

        private String redUtilsUnLockedMessage = "RED_UTILS_UN_LOCKED_";

        private int replicaCount = 0;

        private String hostAddress = DEFAULT_HOST_ADDRESS;

        private int port = DEFAULT_PORT;

        public RedUtilsConfig build() {
            return new RedUtilsConfig(this);
        }

        public RedUtilsConfigBuilder waitingTimeForReplicasMillis(int waitingTimeForReplicasMillis) {
            this.waitingTimeForReplicasMillis = waitingTimeForReplicasMillis;
            return this;
        }

        public RedUtilsConfigBuilder retryCountForSyncingWithReplicas(int retryCountForSyncingWithReplicas) {
            this.retryCountForSyncingWithReplicas = retryCountForSyncingWithReplicas;
            return this;
        }

        public RedUtilsConfigBuilder leaseTimeMillis(int leaseTimeMillis) {
            this.leaseTimeMillis = leaseTimeMillis;
            return this;
        }

        public RedUtilsConfigBuilder readTimeOutMillis(int readTimeOutMillis) {
            this.readTimeOutMillis = readTimeOutMillis;
            return this;
        }

        public RedUtilsConfigBuilder lockMaxPoolSize(int lockMaxPoolSize) {
            this.lockMaxPoolSize = lockMaxPoolSize;
            return this;
        }

        public RedUtilsConfigBuilder channelMaxPoolSize(int channelMaxPoolSize) {
            this.channelMaxPoolSize = channelMaxPoolSize;
            return this;
        }

        public RedUtilsConfigBuilder redUtilsUnLockedMessage(String redUtilsUnLockedMessage) {
            this.redUtilsUnLockedMessage = redUtilsUnLockedMessage;
            return this;
        }

        public RedUtilsConfigBuilder replicaCount(int replicaCount) {
            this.replicaCount = replicaCount;
            return this;
        }

        public RedUtilsConfigBuilder hostAddress(String hostAddress) {
            this.hostAddress = hostAddress;
            return this;
        }

        public RedUtilsConfigBuilder port(int port) {
            this.port = port;
            return this;
        }
    }
}
