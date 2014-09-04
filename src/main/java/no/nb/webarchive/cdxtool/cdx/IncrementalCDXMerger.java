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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.log4j.Logger;

/**
 *
 * @author John Erik Halse
 */
public class IncrementalCDXMerger {
    private static final Logger log = Logger.getLogger(IncrementalCDXMerger.class);

    private final int maxFilesPerRound;

    private final File cdxDir;

    private final File resultCdxFile;

    private final File tmpDir;

    public IncrementalCDXMerger(final File cdxDir, final File resultCdxFile, int maxFilesPerRound) {
        this.cdxDir = cdxDir;
        this.resultCdxFile = resultCdxFile;
        this.maxFilesPerRound = maxFilesPerRound;
        this.tmpDir = new File(cdxDir, "tmpMergeDir");
    }

    public void execute() throws IOException {
        log.info("Starting to merge CDX files");
        long start = System.currentTimeMillis();

        File[] cdxFiles = findFiles();
        if (cdxFiles != null && cdxFiles.length > 0) {
            tmpDir.mkdirs();

            File result = merge(cdxFiles, 0);

            resultCdxFile.delete();

            if (!result.renameTo(resultCdxFile)) {
                throw new IOException("Rename of mergefile failed");
            }

            tmpDir.delete();
        }

        log.info("Finished merging " + cdxFiles.length + " CDX files in " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
    }

    private File merge(File[] cdxFiles, int depth) throws IOException {
        File[][] filesPerRound = splitFileArray(cdxFiles);
        File[] outFiles = new File[filesPerRound.length];
        File resultFile;

        for (int i = 0; i < filesPerRound.length; i++) {
            File[] thisRound = filesPerRound[i];
            outFiles[i] = new File(tmpDir, depth + "-" + i + ".cdx");
            Merger merger = new Merger(thisRound, new PrintWriter(new BufferedWriter(new FileWriter(outFiles[i]), 1024 * 1024)), depth == 0);

            if (depth > 0) {
                for (File oFile : thisRound) {
                    oFile.delete();
                }
            }
        }

        if (filesPerRound.length > 1) {
            resultFile = merge(outFiles, depth + 1);
        } else {
            resultFile = outFiles[0];
        }

        return resultFile;
    }

    final File[][] splitFileArray(File[] files) {
        int fullArrayCount = files.length / maxFilesPerRound;
        int remaining = files.length % maxFilesPerRound;
        File[][] result = new File[remaining > 0 ? fullArrayCount + 1 : fullArrayCount][];
        for (int i = 0; i < fullArrayCount; i++) {
            result[i] = new File[maxFilesPerRound];
            System.arraycopy(files, i * maxFilesPerRound, result[i], 0, maxFilesPerRound);
        }

        if (remaining > 0) {
            result[fullArrayCount] = new File[remaining];
            System.arraycopy(files, fullArrayCount * maxFilesPerRound, result[fullArrayCount], 0, remaining);
        }

        return result;
    }

    final File[] findFiles() {
        File[] cdxFiles = cdxDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cdx");
            }
        });

        return cdxFiles;
    }
}
