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

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * An ARC/WARC file which might be processed.
 *
 * @author John Erik Halse
 */
public class SourceFileCandidate implements Delayed, Runnable {

    private final FileProcessingAction processingAction;

    private final File file;

    private long lastCheckSize;

    private long nextCheckTime;

    private final int checkIntervalMillis;

    private boolean busy = true;

    private boolean handled = false;

    public SourceFileCandidate(FileProcessingAction processingAction, File file, int checkIntervalMillis) {
        this.processingAction = processingAction;
        this.file = file;
        this.checkIntervalMillis = checkIntervalMillis;
        checkBusy();
    }

    /**
     * Check if the file is ready for processing, that is the file is not growing.
     *
     * @return true if file can be processed
     */
    public boolean isBusy() {
        checkBusy();
        return busy;
    }

    public boolean isHandled() {
        return handled;
    }

    public File getFile() {
        return file;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long delay;
        if (!busy) {
            delay = 0;
        } else {
            delay = unit.convert(nextCheckTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        return delay;
    }

    @Override
    public int compareTo(Delayed o) {
        return (int) (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    /**
     * Check if the file has changed size during the last check interval.
     */
    private void checkBusy() {
        if (nextCheckTime > System.currentTimeMillis()) {
            busy = true;
        } else if (lastCheckSize == 0L) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            if (file.lastModified() < cal.getTimeInMillis()) {
                busy = false;
            } else {
                busy = true;
                updateValues();
            }
        } else {
            if (lastCheckSize == file.length()) {
                busy = false;
            } else {
                busy = true;
                updateValues();
            }
        }
    }

    private void updateValues() {
        lastCheckSize = file.length();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, checkIntervalMillis);
        nextCheckTime = cal.getTimeInMillis();
    }

    @Override
    public void run() {
        if (!isBusy()) {
            // Do the processing
            processingAction.setFile(this);
            processingAction.run();
            handled = true;
        }
    }

}
