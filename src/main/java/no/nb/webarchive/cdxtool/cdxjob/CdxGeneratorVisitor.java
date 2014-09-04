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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import no.nb.webarchive.cdxtool.Config;
import no.nb.webarchive.cdxtool.FileVisitor;
import no.nb.webarchive.cdxtool.ResourceIndexHelper;
import org.apache.log4j.Logger;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXFormatIndex;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXFormatAdapter;
import org.archive.wayback.resourceindex.cdx.format.CDXFormat;
import org.archive.wayback.resourceindex.cdx.format.CDXFormatException;
import org.archive.wayback.resourcestore.indexer.IndexWorker;
import org.archive.util.iterator.CloseableIterator;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.archive.wayback.util.url.IdentityUrlCanonicalizer;
import org.archive.wayback.util.url.KeyMakerUrlCanonicalizer;

/**
 *
 * @author John Erik Halse
 */
public class CdxGeneratorVisitor implements FileVisitor {

    private static final Logger log = Logger.getLogger(CdxGeneratorVisitor.class);

    private final Config config;

    private Runnable preProcessTask;

    private Runnable postProcessTask;

    // Må ha denne fordi filsystem er montert med annen path på appserv
    private final File waybackViewBaseDir = new File("/wayback/waybackup/arcfiles1");

    private ResourceIndexHelper db;

    private BusyCheckingCompletionService<SourceFileCandidate> ecs;

    private final AtomicInteger submitCount = new AtomicInteger();

    private final AtomicLong size = new AtomicLong();

    private final AtomicLong fileCount = new AtomicLong();

    public CdxGeneratorVisitor(Config config) {
        System.out.println("Starting cdx generator visitor ....");
        this.config = config;

        if (config.getOutputDirectory() == null) {
            throw new NullPointerException("No output directory defined");
        }

        if (config.getMergedIndexFile() == null) {
            throw new NullPointerException("No merged index file defined");
        }

        if (config.getResourceIndexFile() == null) {
            throw new NullPointerException("No resource index file defined");
        }

        if (!config.getOutputDirectory().exists()) {
            config.getOutputDirectory().mkdirs();
        }

        db = new ResourceIndexHelper(config.getResourceIndexFile());

        ecs = new BusyCheckingCompletionService<SourceFileCandidate>();
        System.out.println("Cdx generator visitor started");
    }

    public void setPreProcessTask(Runnable preProcessTask) {
        this.preProcessTask = preProcessTask;
    }

    public void setPostProcessTask(Runnable postProcessTask) {
        this.postProcessTask = postProcessTask;
    }

    @Override
    public void visit(File file) {
        FileProcessingAction processingAction = new FileProcessingAction() {
            @Override
            public void run() {
                try {
                    executeIndex(getFile());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        };

        SourceFileCandidate fileJob = new SourceFileCandidate(processingAction, file, 1000);
        submitCount.incrementAndGet();
        ecs.submit(fileJob, fileJob);
    }

    void executeIndex(SourceFileCandidate sourceFileCandidate) throws IOException {
        File sourceFile = sourceFileCandidate.getFile();
        File cdxFile = new File(config.getOutputDirectory(), sourceFile.getName() + ".cdx");

        if (!cdxFile.exists()) {
            try {
                Collection<String> cdxData = generateCDX(sourceFile, cdxFile);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                cdxFile.delete();
                return;
            }
        }

        if (db != null) {
            db.visited(sourceFile);
        }

        fileCount.incrementAndGet();
        size.getAndAdd(sourceFileCandidate.getFile().length());
    }

    private Collection<String> generateCDX(File input, File output) throws IOException {
        String cdxSpec = CDXFormatIndex.CDX_HEADER_MAGIC;
        UrlCanonicalizer canonicalizer = new AggressiveUrlCanonicalizer();

        if (config.isIdentity()) {
            canonicalizer = new IdentityUrlCanonicalizer();
        } else if (config.isNewCanonClassic()) {
            canonicalizer = new KeyMakerUrlCanonicalizer(false);
            cdxSpec = CDXFormatIndex.CDX_HEADER_MAGIC_NEW;
        } else if (config.isNewCanonSurt()) {
            canonicalizer = new KeyMakerUrlCanonicalizer(true);
            cdxSpec = CDXFormatIndex.CDX_HEADER_MAGIC_NEW;
        } else if (config.getCdxSpec() != null) {
            canonicalizer = new AggressiveUrlCanonicalizer();
            cdxSpec = config.getCdxSpec();
        }

		if(config.getCdxSpec() == null && config.isIdentity()) {
			cdxSpec = cdxSpec.replace(" N ", " a ");
		}

        IndexWorker worker = new IndexWorker();
        worker.setCanonicalizer(canonicalizer);
        worker.setInterval(0);
        worker.init();
        try {
            Collection<String> cdxData = new ArrayList<String>();
            cdxData.add(cdxSpec);

            CloseableIterator<CaptureSearchResult> itr = worker.indexFile(input.getCanonicalPath());
            CDXFormat cdxFormat = new CDXFormat(cdxSpec);

            Iterator<String> lines = SearchResultToCDXFormatAdapter.adapt(itr, cdxFormat);

            if (lines == null) {
                throw new IOException("Bad file: " + input);
            }

            try {
                while (lines.hasNext()) {
                    cdxData.add(lines.next());
                }
            } catch (Exception e) {
                throw new IOException("Bad file: " + input);
            }

            PrintWriter pw = new PrintWriter(output);
            for (String line : cdxData) {
                pw.println(line);
            }
            pw.close();

            log.debug("Generated CDX for '" + input + "'");
            return cdxData;
        } catch (CDXFormatException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void preProcess() {
        submitCount.set(0);
        size.set(0);
        fileCount.set(0);

        if (preProcessTask != null) {
            preProcessTask.run();
        }
    }

    @Override
    public synchronized void postProcess() {
        for (int i = 0; i < submitCount.get(); i++) {
            try {
                ecs.take().get(5, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                log.debug("Waiting for " + ecs.getPendingJobCount() + " to finish.");
                i--;
            } catch (ExecutionException ex) {
                log.fatal(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                log.fatal(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }

        if (postProcessTask != null) {
            postProcessTask.run();
        }

    }

    public long getFileCount() {
        return fileCount.get();
    }

    public long getTotalSize() {
        return size.get();
    }

    @Override
    public String toString() {
        long s;
        String t;
        if (size.get() < 1024) {
            s = size.get();
            t = "bytes";
        } else if (size.get() < (1048576)) {
            s = size.get() / 1024;
            t = "KB";
        } else if (size.get() < (1073741824)) {
            s = size.get() / (1048576);
            t = "MB";
        } else {
            s = size.get() / (1073741824);
            t = "GB";
        }

        return String.format("Files visited: %d, Files copied: %d, Size: %d %s", submitCount.get(), fileCount.get(), s, t);
    }

}
