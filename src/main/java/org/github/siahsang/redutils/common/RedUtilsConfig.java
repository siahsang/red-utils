package org.github.siahsang.redutils.common;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Javad Alimohammadi
 */

public class RedUtilsConfig {
    public static final String DEFAULT_HOST_ADDRESS = "127.0.0.1";

    public static final int DEFAULT_PORT = 6379;

    private static final String REDIS_URI_PREFIX = "redis://";

    private final int waitingTimeForReplicasMillis;

    private final int retryCountForSyncingWithReplicas;

    private final int leaseTimeMillis;

    private final int readTimeOutMillis;

    private final int lockMaxPoolSize;

    private final String unlockedMessagePattern;

    private final int replicaCount;

    private final URI uri;

    private RedUtilsConfig(RedUtilsConfigBuilder redUtilsConfigBuilder) {
        this.waitingTimeForReplicasMillis = redUtilsConfigBuilder.waitingTimeForReplicasMillis;
        this.retryCountForSyncingWithReplicas = redUtilsConfigBuilder.retryCountForSyncingWithReplicas;
        this.leaseTimeMillis = redUtilsConfigBuilder.leaseTimeMillis;
        this.readTimeOutMillis = redUtilsConfigBuilder.readTimeOutMillis;
        this.lockMaxPoolSize = redUtilsConfigBuilder.maxPoolSize;
        this.unlockedMessagePattern = redUtilsConfigBuilder.redUtilsUnLockedMessage;
        this.replicaCount = redUtilsConfigBuilder.replicaCount;
        this.uri = redUtilsConfigBuilder.parseUri();
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

    public String getUnlockedMessagePattern() {
        return unlockedMessagePattern;
    }

    public int getReplicaCount() {
        return replicaCount;
    }

    public URI getUri() {
        return uri;
    }

    public static final class RedUtilsConfigBuilder {
        private int waitingTimeForReplicasMillis = 1000;

        private int retryCountForSyncingWithReplicas = 3;

        private int leaseTimeMillis = 30_000;

        private int readTimeOutMillis = 2000;

        private int maxPoolSize = 60;

        private String redUtilsUnLockedMessage = "RED_UTILS_UN_LOCKED_";

        private int replicaCount = 0;

        private String hostAddress = DEFAULT_HOST_ADDRESS;

        private int port = DEFAULT_PORT;

        private URI uri = null;

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

        public RedUtilsConfigBuilder maxPoolSize(int lockMaxPoolSize) {
            this.maxPoolSize = lockMaxPoolSize;
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

        public RedUtilsConfigBuilder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public RedUtilsConfigBuilder uri(String uri) {
            this.uri = URI.create(uri);
            return this;
        }

        private URI parseUri() {
            return uri != null ? uri : URI.create(REDIS_URI_PREFIX + hostAddress + ":" + port);
        }
    }
}
