package in.xnnyygn.xraft.core.node;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import in.xnnyygn.xraft.core.log.Log;
import in.xnnyygn.xraft.core.rpc.ChannelException;
import in.xnnyygn.xraft.core.rpc.Connector;
import in.xnnyygn.xraft.core.schedule.Scheduler;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class NodeContext {

    private static final Logger logger = LoggerFactory.getLogger(NodeContext.class);

    private final NodeId selfNodeId;
    private final NodeGroup nodeGroup;
    private final NodeStore nodeStore;
    private final EventBus eventBus;
    private final ExecutorService monitorExecutorService;

    private Log log;
    private Scheduler scheduler;
    private Connector connector;

    private boolean standbyMode = false;

    public NodeContext(NodeId selfNodeId, NodeGroup nodeGroup, NodeStore nodeStore, EventBus eventBus) {
        this.selfNodeId = selfNodeId;
        this.nodeGroup = nodeGroup;
        this.nodeStore = nodeStore;
        this.eventBus = eventBus;
        this.monitorExecutorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "monitor-" + selfNodeId));
    }

    public void initialize() {
        connector.initialize();
        eventBus.register(this);
    }

    public boolean isStandbyMode() {
        return standbyMode;
    }

    public void setStandbyMode(boolean standbyMode) {
        this.standbyMode = standbyMode;
    }

    public NodeId getSelfNodeId() {
        return this.selfNodeId;
    }

    public NodeGroup getNodeGroup() {
        return nodeGroup;
    }

    public void resetReplicationStates() {
        nodeGroup.resetReplicationStates(selfNodeId, log);
    }

    public void addNode(NodeEndpoint config, boolean memberOfMajor) {
        nodeGroup.addNode(config, log.getNextIndex(), memberOfMajor);
    }

    public NodeStore getNodeStore() {
        return nodeStore;
    }

    public Log getLog() {
        return log;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public void register(Object eventSubscriber) {
        this.eventBus.register(eventSubscriber);
    }

    public Future<?> runWithMonitor(ListeningExecutorService executorService, Runnable r) {
        ListenableFuture<?> future = executorService.submit(r);
        monitor(future);
        return future;
    }

    public <V> Future<V> runWithMonitor(ListeningExecutorService executorService, Callable<V> c) {
        ListenableFuture<V> future = executorService.submit(c);
        monitor(future);
        return future;
    }

    private <V> void monitor(ListenableFuture<V> future) {
        Futures.addCallback(future, new FutureCallback<V>() {
            @Override
            public void onSuccess(@Nullable V result) {
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof ChannelException) {
                    logger.warn(t.getMessage());
                } else {
                    logger.warn("failure", t);
                }
            }
        }, monitorExecutorService);
    }

    @Subscribe
    public void onReceive(DeadEvent deadEvent) {
        logger.warn("dead event {}", deadEvent);
    }

    public void release() throws InterruptedException {
        nodeStore.close();
        log.close();
        scheduler.stop();
        connector.close();
        monitorExecutorService.shutdown();
        monitorExecutorService.awaitTermination(1L, TimeUnit.SECONDS);
    }

}
