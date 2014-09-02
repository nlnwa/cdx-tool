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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import no.nb.webarchive.cdxtool.cdx.IncrementalCDXMerger;
import no.nb.webarchive.cdxtool.cdxjob.CdxGeneratorVisitor;

/**
 * Main class for the CdxTool.
 *
 * @author John Erik Halse
 */
public class CdxTool {

    private static final Logger log = Logger.getLogger(CdxTool.class.getName());

    private Config config;

    public CdxTool() {
    }

    public void setConfig(final Config config) throws InterruptedException, ExecutionException, IOException {
        this.config = config;
    }

    @PostConstruct
    public void execute() {
        if (config == null) {
            throw new IllegalStateException("No config was set");
        }

        SourceDirectories sourceDirectories = new SourceDirectories();
        sourceDirectories.addSourceDirectories(config.getSourceDirectories());

        CdxGeneratorVisitor fileVisitor = new CdxGeneratorVisitor(config);

        if (config.isShouldMerge()) {
            final IncrementalCDXMerger cdxMerger = new IncrementalCDXMerger(config.getOutputDirectory(), config.getMergedIndexFile(), 500);

            fileVisitor.setPreProcessTask(new Runnable() {
                @Override
                public void run() {
                    if (!config.getMergedIndexFile().exists()) {
                        try {
                            log.info("Merging existing CDX files from " + config.getOutputDirectory().getCanonicalPath());
                            cdxMerger.execute();
                            log.info("Creating CDX files for ARC/WARC files");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

            });

            fileVisitor.setPostProcessTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        cdxMerger.execute();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });
        }

        Scheduler scheduler = new Scheduler(sourceDirectories, fileVisitor);
        if (config.getScanIntervalSeconds() > 0) {
            try {
                try {
                    scheduler.start(config.getScanIntervalSeconds(), TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            } finally {
                scheduler.stop();
            }
        } else {
            scheduler.runOnce();
        }
    }

}
