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
package no.nb.webarchive.cdxtool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author John Erik Halse
 */
public class Scheduler {

    private static final Logger log = Logger.getLogger(Scheduler.class.getName());

    private ScheduledExecutorService executorService;

    private final SourceDirectories sourceDirectories;

    private final FileVisitor fileVisitor;

    public Scheduler(SourceDirectories sourceDirectories, FileVisitor fileVisitor) {
        this.sourceDirectories = sourceDirectories;
        this.fileVisitor = fileVisitor;
    }

    public void start(long period, TimeUnit timeUnit, boolean returnImmediately) {
        executorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture future = executorService.scheduleAtFixedRate(new Worker(), 0, period, timeUnit);
        if (!returnImmediately) {
            try {
                future.get();
            } catch (InterruptedException ex) {
                stop();
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                stop();
                throw new RuntimeException(ex);
            }
        }
    }

    public void runOnce() {
        new Worker().run();
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            log.info("Starting job ...");
            fileVisitor.preProcess();
            sourceDirectories.walk(fileVisitor);
            try {
                fileVisitor.postProcess();
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
            }
            log.info("Finished job.");
            log.info(fileVisitor.toString());
        }

    }
}
