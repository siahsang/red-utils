package org.github.siahsang.test.redis;

import org.github.siahsang.redutils.RedUtilsLockImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class RedisServer {
    private static final Logger log = LoggerFactory.getLogger(RedUtilsLockImpl.class);

    private final Path RESOURCE_PATH = Paths.get("src/test/resources");

    public ImageFromDockerfile buildImageDockerfile() {
        return new ImageFromDockerfile("redutils-redis-image_tmp", false)
                .withFileFromPath(".", RESOURCE_PATH);
    }

    public final static int DEFAULT_CONTAINER_MASTER_PORT = 6379;

    private GenericContainer<?> redisContainer;

    public RedisAddress startSingleInstance() {
        return startRedis(0, AOFConfiguration.ALWAYS);
    }

    public RedisAddress startMasterReplicas(final int replicaCount) {
        return startRedis(replicaCount, AOFConfiguration.ALWAYS);
    }


    public RedisAddress startRedis(final int replicaCount, final AOFConfiguration AOFConfig) {
        String enableAOFStr = "no";

        if (!AOFConfig.equals(AOFConfiguration.NO)) {
            enableAOFStr = "yes";
        }

        Integer[] exposedPorts = new Integer[1 + replicaCount];

        exposedPorts[0] = DEFAULT_CONTAINER_MASTER_PORT;

        for (int i = 1; i <= replicaCount; i++) {
            exposedPorts[i] = DEFAULT_CONTAINER_MASTER_PORT + i;
        }

        redisContainer = new GenericContainer(buildImageDockerfile())
                .withEnv("MASTER_PORT", String.valueOf(DEFAULT_CONTAINER_MASTER_PORT))
                .withEnv("REPLICAS", String.valueOf(replicaCount))
                .withEnv("AOF", enableAOFStr)
                .withEnv("AOF_CONFIG", AOFConfig.value)
                .withExposedPorts(exposedPorts);

        redisContainer.start();

        return new RedisAddress(redisContainer.getFirstMappedPort(), redisContainer.getHost());
    }

    public void shutDown() {
        redisContainer.stop();
    }

    public void shutdownMaster() throws IOException, InterruptedException {
        redisContainer.execInContainer("redis-cli", "-p", String.valueOf(DEFAULT_CONTAINER_MASTER_PORT), "SHUTDOWN");
    }

    public void shutdownReplica(final int replicaNumber) throws IOException, InterruptedException {
        List<Integer> exposedPorts = redisContainer.getExposedPorts();
        raiseExceptionIfNoReplicaAvailable();

        final int port = replicaNumber + 1; // because first port is for master
        log.info("Shutting Down replica no [{}]", replicaNumber);
        redisContainer.execInContainer("redis-cli", "-p", String.valueOf(exposedPorts.get(port)), "SHUTDOWN");
    }

    public void pauseMaster(final int seconds) throws IOException, InterruptedException {
        List<Integer> exposedPorts = redisContainer.getExposedPorts();
        final String pauseTimeInMillis = String.valueOf(seconds * 1000);
        log.info("Pausing Master for [{}] second(s)", seconds);
        redisContainer.execInContainer("redis-cli", "-p",
                String.valueOf(exposedPorts.get(0)), "CLIENT", "PAUSE", pauseTimeInMillis);
    }


    public void pauseReplica(final int replicaNumber, final int seconds) throws IOException, InterruptedException {
        List<Integer> exposedPorts = redisContainer.getExposedPorts();
        raiseExceptionIfNoReplicaAvailable();

        final int port = replicaNumber + 1; // because first port is for master
        final String pauseTimeInMillis = String.valueOf(seconds * 1000);
        log.info("Pausing replica number [{}] for [{}] second(s)", replicaNumber, seconds);
        redisContainer.execInContainer("redis-cli", "-p",
                String.valueOf(exposedPorts.get(port)), "CLIENT", "PAUSE", pauseTimeInMillis);
    }


    private void raiseExceptionIfNoReplicaAvailable() {
        List<Integer> exposedPorts = redisContainer.getExposedPorts();
        if (exposedPorts.size() <= 1) {
            throw new IllegalArgumentException("There are no any replica");
        }
    }
}
