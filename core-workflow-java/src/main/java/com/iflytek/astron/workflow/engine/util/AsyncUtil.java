package com.iflytek.astron.workflow.engine.util;

import cn.hutool.core.thread.ExecutorBuilder;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步工具类
 *
 * @author YiHui
 * @date 2023/6/12
 */
@Slf4j
public class AsyncUtil {
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread thread = this.defaultFactory.newThread(r);
            if (!thread.isDaemon()) {
                thread.setDaemon(true);
            }

            thread.setName("workflow-" + this.threadNumber.getAndIncrement());
            return thread;
        }
    };
    private static ExecutorService executorService;
    private static SimpleTimeLimiter simpleTimeLimiter;

    static {
        initExecutorService(Runtime.getRuntime().availableProcessors() * 2, 50);
    }

    public static void initExecutorService(int core, int max) {
        // 异步工具类的默认线程池构建, 参数选择原则:
        //  1. 技术派不存在cpu密集型任务，大部分操作都设计到 redis/mysql 等io操作
        //  2. 统一的异步封装工具，这里的线程池是一个公共的执行仓库，不希望被其他的线程执行影响，因此队列长度为0, 核心线程数满就创建线程执行，超过最大线程，就直接当前线程执行
        //  3. 同样因为属于通用工具类，再加上技术派的异步使用的情况实际上并不是非常饱和的，因此空闲线程直接回收掉即可；大部分场景下，cpu * 2的线程数即可满足要求了
        max = Math.max(core, max);
        executorService = new ExecutorBuilder()
                .setCorePoolSize(core)
                .setMaxPoolSize(max)
                .setKeepAliveTime(0)
                .setKeepAliveTime(0, TimeUnit.SECONDS)
                .setWorkQueue(new SynchronousQueue<Runnable>())
                .setHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .setThreadFactory(THREAD_FACTORY)
                .buildFinalizable();
        // 包装一下线程池，避免出现上下文复用场景
        executorService = TtlExecutors.getTtlExecutorService(executorService);
        simpleTimeLimiter = SimpleTimeLimiter.create(executorService);
    }


    /**
     * 带超时时间的方法调用执行，当执行时间超过给定的时间，则返回一个超时异常，内部的任务还是正常执行
     * 若超时时间内执行完毕，则直接返回
     *
     * @param time
     * @param unit
     * @param call
     * @param <T>
     * @return
     */
    public static <T> T callWithTimeLimit(long time, TimeUnit unit, Callable<T> call) throws ExecutionException, InterruptedException, TimeoutException {
        return simpleTimeLimiter.callWithTimeout(call, time, unit);
    }
    public static void execute(Runnable call) {
        executorService.execute(call);
    }

    public static <T> Future<T> submit(Callable<T> t) {
        return executorService.submit(t);
    }

    // 新增一个sleep的方法
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("线程被中断", e);
            Thread.currentThread().interrupt();
        }
    }
}


