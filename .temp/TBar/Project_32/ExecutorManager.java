package cn.myperf4j.base.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by LinShunkang on 2018/8/23
 * <p>
 * 该类用于统一管理ExecutorService，便于统一关闭线程池
 */
public final class ExecutorManager {

    private static final Set<ExecutorService> executors = new HashSet<>();

    private ExecutorManager() {
        //empty
    }

    public static void addExecutorService(ExecutorService executor) {
        executors.add(executor);
    }

    public static void stopAll(long timeout, TimeUnit unit) {
        for (ExecutorService executorService : executors) {
            try {
                executorService.shutdown();
                executorService.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                Logger.error("ExecutorManager.stopAll()", e);
            }
        }
    }
}
