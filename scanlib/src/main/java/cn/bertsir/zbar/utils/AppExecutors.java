package cn.bertsir.zbar.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: ShiLiang
 * @describe: 线程池
 */

public class AppExecutors {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    //核心线程数
    private static final int THREAD_COUNT = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    //空闲线程存活时间
    private static final int KEEP_ALIVE_TIME = 5;

    //单例
    private static final AppExecutors APP_EXECUTORS = new AppExecutors();

    // single
    private final Executor diskIO;

    // IO 线程
    private final Executor networkIO;

    //主线程
    private final Executor mainThread;

    private AppExecutors(Executor singleIO, Executor networkIO, Executor mainThread) {
        this.diskIO = singleIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors getInstance(){
        return APP_EXECUTORS;
    }

    private AppExecutors() {
        this(new DiskIOThreadExecutor(), new NetWorkThreadExecutor(), new MainThreadExecutor());
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    private static class DiskIOThreadExecutor implements Executor {

        private final Executor diskIO;

        private DiskIOThreadExecutor() {
            diskIO = Executors.newSingleThreadExecutor();
        }

        @Override
        public void execute(@NonNull Runnable command) {
            diskIO.execute(command);
        }
    }

    private static class NetWorkThreadExecutor implements Executor {

        private final Executor netWorkIO;

        private NetWorkThreadExecutor() {
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(THREAD_COUNT, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            threadPoolExecutor.allowCoreThreadTimeOut(true);//允许核心线程销毁
            netWorkIO = threadPoolExecutor;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            netWorkIO.execute(command);
        }
    }
}