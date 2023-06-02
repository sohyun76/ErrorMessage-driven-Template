package cn.myperf4j.base.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LinShunkang on 2018/3/16
 */
public final class ThreadUtils {

    private ThreadUtils() {
        //empty
    }

    public static ThreadFactory newThreadFactory(final String prefix) {
        return new ThreadFactory() {
            final AtomicInteger threadId = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, prefix + threadId.getAndIncrement());
            }
        };
    }

    public static void sleepQuietly(long time, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(time);
        } catch (Exception e) {
            //empty
        }
    }
}
