package de.redstonecloud.cloud.utils;

import lombok.Builder;
import lombok.NonNull;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
public final class ThreadFactoryBuilder implements ThreadFactory {
    private static final ThreadFactory backingFactory = Executors.defaultThreadFactory();

    private final AtomicInteger count = new AtomicInteger(0);
    private final boolean daemon;
    private final String format;
    @Builder.Default
    private final int priority = Thread.currentThread().getPriority();
    private final Thread.UncaughtExceptionHandler exceptionHandler;

    private static String format(String format, int count) {
        return String.format(Locale.ROOT, format, count);
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = backingFactory.newThread(runnable);

        if (format != null) {
            thread.setName(format(format, count.getAndIncrement()));
        }

        thread.setDaemon(daemon);
        thread.setPriority(priority);

        if (exceptionHandler != null) {
            thread.setUncaughtExceptionHandler(exceptionHandler);
        }
        return thread;
    }
}