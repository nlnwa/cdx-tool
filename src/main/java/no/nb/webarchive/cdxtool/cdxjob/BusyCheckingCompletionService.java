/*
 * Copyright 2014 National Library of Norway.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.nb.webarchive.cdxtool.cdxjob;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author John Erik Halse
 */
public class BusyCheckingCompletionService<V extends SourceFileCandidate> implements CompletionService<V> {
    private static final int THREADPOOL_SIZE = 10;
    private final ThreadPoolExecutor executor;

    private final BlockingQueue<Future<V>> completionQueue;

    private class DynamicFutureTask extends FutureTask<V> implements Delayed {

        final SourceFileCandidate command;

        public DynamicFutureTask(V command) {
            super(command, command);
            this.command = command;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return command.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return command.compareTo(o);
        }

        public boolean isBusy() {
            return command.isBusy();
        }

        @Override
        protected void done() {
            completionQueue.add(this);
        }
    }

    private class DynamicDelayQueue<V extends DynamicFutureTask> extends DelayQueue<V> {

        void requeue(V object) {
        }

        @Override
        public V poll() {
            V value = super.poll();
            if (value != null) {
                if (value.isBusy()) {
                    offer(value);
                    value = null;
                }
            }
            return value;
        }

        @Override
        public V take() throws InterruptedException {
            V value = super.take();
            if (value.isBusy()) {
                offer(value);
                value = take();
            }
            return value;
        }

        @Override
        public V poll(long timeout, TimeUnit unit) throws InterruptedException {
            V value = super.poll(timeout, unit);
            if (value != null) {
                if (value.isBusy()) {
                    offer(value);
                    value = null;
                }
            }
            return value;
        }
        
    }

    public BusyCheckingCompletionService() {
        this.executor = new ThreadPoolExecutor(THREADPOOL_SIZE, THREADPOOL_SIZE, 60, TimeUnit.SECONDS, new DynamicDelayQueue());
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();
    }

    @Override
    public Future<V> submit(Callable<V> task) {
        throw new UnsupportedOperationException();
//        if (task == null) {
//            throw new NullPointerException();
//        }
//        RunnableFuture<V> f = newTaskFor(task);
//        executor.execute(new DynamicFutureTask(result));
//        return f;
    }

    @Override
    public Future<V> submit(Runnable task, V result) {
        if (task == null) {
            throw new NullPointerException();
        }
        DynamicFutureTask f = new DynamicFutureTask(result);
        delayedExecute(f);
        return f;
    }
    
    /**
     * Specialized variant of ThreadPoolExecutor.execute for delayed tasks.
     */
    private void delayedExecute(Runnable command) {
        if (executor.isShutdown()) {
//            executor.reject(command);
            return;
        }
        // Prestart a thread if necessary. We cannot prestart it
        // running the task because the task (probably) shouldn't be
        // run yet, so thread will just idle until delay elapses.
        if (executor.getPoolSize() < executor.getCorePoolSize())
            executor.prestartCoreThread();

        executor.getQueue().add(command);
    }
    
    @Override
    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }

    @Override
    public Future<V> poll() {
        return completionQueue.poll();
    }

    @Override
    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }
    
    public int getPendingJobCount() {
        return executor.getQueue().size();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
