package in.xnnyygn.xraft.core.schedule;

import in.xnnyygn.xraft.core.node.NodeConfig2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultScheduler implements Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultScheduler.class);
    private final int minElectionTimeout;
    private final int maxElectionTimeout;
    private final int logReplicationDelay;
    private final int logReplicationInterval;
    private final Random electionTimeoutRandom;
    private final ScheduledExecutorService scheduledExecutor;

    @Deprecated
    public DefaultScheduler() {
        this(4000, 5000, 0, 1000);
    }

    public DefaultScheduler(NodeConfig2 config) {
        this(config.getMinElectionTimeout(), config.getMaxElectionTimeout(), config.getLogReplicationDelay(),
                config.getLogReplicationInterval());
    }

    public DefaultScheduler(int minElectionTimeout, int maxElectionTimeout, int logReplicationDelay, int logReplicationInterval) {
        if (minElectionTimeout <= 0 || maxElectionTimeout <= 0 || minElectionTimeout > maxElectionTimeout) {
            throw new IllegalArgumentException("election timeout should not be 0 or min > max");
        }
        if (logReplicationDelay < 0 || logReplicationInterval <= 0) {
            throw new IllegalArgumentException("log replication delay < 0 or log replication interval <= 0");
        }
        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.logReplicationDelay = logReplicationDelay;
        this.logReplicationInterval = logReplicationInterval;
        electionTimeoutRandom = new Random();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "scheduler"));
    }

    @Override
    public LogReplicationTask scheduleLogReplicationTask(Runnable task) {
        logger.debug("schedule log replication task");
        ScheduledFuture<?> scheduledFuture = this.scheduledExecutor.scheduleWithFixedDelay(
                task, logReplicationDelay, logReplicationInterval, TimeUnit.MILLISECONDS);
        return new LogReplicationTask(scheduledFuture);
    }

    @Override
    public ElectionTimeout scheduleElectionTimeout(Runnable task) {
        logger.debug("schedule election timeout");
        int timeout = electionTimeoutRandom.nextInt(minElectionTimeout) + (maxElectionTimeout - minElectionTimeout);
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(task, timeout, TimeUnit.MILLISECONDS);
        return new ElectionTimeout(scheduledFuture, () -> scheduleElectionTimeout(task));
    }

    @Override
    public void stop() throws InterruptedException {
        logger.debug("stop scheduler");
        this.scheduledExecutor.shutdown();
        this.scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }

}
