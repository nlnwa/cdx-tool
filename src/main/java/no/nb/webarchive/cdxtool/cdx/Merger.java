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
package no.nb.webarchive.cdxtool.cdx;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author John Erik Halse
 */
public class Merger {

    private static final Logger log = Logger.getLogger(Merger.class.getName());

    private final List<LineReader> mergeReaders = new ArrayList<LineReader>();

    private final PrintWriter outWriter;

    public Merger(File[] filesToMerge, File output, boolean sortInput) throws IOException {
        this(filesToMerge, new PrintWriter(output), sortInput);
    }

    public Merger(File[] filesToMerge, PrintWriter output, boolean sortInput) throws IOException {
        for (int i = 0; i < filesToMerge.length; i++) {
            if (sortInput) {
                mergeReaders.add(new SortedLineReader(filesToMerge[i]));
            } else {
                mergeReaders.add(new LineReader(filesToMerge[i]));
            }
        }
        outWriter = output;

        merge();

        outWriter.close();
    }

    public final void merge() throws IOException {
        while (!mergeReaders.isEmpty()) {
            LineReader bestSoFar = null;
            for (Iterator<LineReader> it = mergeReaders.iterator(); it.hasNext();) {
                LineReader mergeFile = it.next();
                if (mergeFile.hasMore()) {
                    if (bestSoFar == null) {
                        bestSoFar = mergeFile;
                    } else if (bestSoFar.isGreatherThan(mergeFile)) {
                        bestSoFar = mergeFile;
                    }
                } else {
                    it.remove();
                }
            }

            if (bestSoFar != null) {
                outWriter.println(bestSoFar.next());
            }
        }
    }
}
