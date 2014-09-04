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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.archive.wayback.resourceindex.cdx.CDXFormatIndex;

/**
 *
 * @author John Erik Halse
 */
public class Config {

    private final List<SourceDirectory> sourceDirectories = new ArrayList<SourceDirectory>();

    private String[] sourceFileSuffixes = new String[] {".arc", ".warc", ".arc.gz", ".warc.gz"};

    private boolean recursive = false;

    private File resourceIndexFile = new File("path-index.txt");

    private File mergedIndexFile = new File("index.cdx");

    private File outputDirectory = null;

    private int scanIntervalSeconds = 0;

    private boolean shouldMerge = false;

    private String cdxSpec;

    private boolean identity = false;

    private boolean newCanonClassic = false;

    private boolean newCanonSurt = false;

    public File getResourceIndexFile() {
        return resourceIndexFile;
    }

    public void setResourceIndexFile(File resourceIndexFile) {
        this.resourceIndexFile = resourceIndexFile;
    }

    public File getMergedIndexFile() {
        return mergedIndexFile;
    }

    public void setMergedIndexFile(File mergedIndexFile) {
        this.mergedIndexFile = mergedIndexFile;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
        for (SourceDirectory sourceDirectory : sourceDirectories) {
            sourceDirectory.setRecursive(recursive);
        }
    }

    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public String[] getSourceFileSuffixes() {
        return sourceFileSuffixes;
    }

    public void setSourceFileSuffixes(String... sourceFileSuffixes) {
        this.sourceFileSuffixes = sourceFileSuffixes;
        for (SourceDirectory sourceDirectory : sourceDirectories) {
            sourceDirectory.setFilenNameSuffixes(sourceFileSuffixes);
        }
    }

    public List<SourceDirectory> getSourceDirectories() {
        return sourceDirectories;
    }

    public void addSourceDirectory(File directory) {
        SourceDirectory sourceDirectory = new SourceDirectory(directory, recursive);
        sourceDirectory.setRecursive(recursive);
        sourceDirectory.setFilenNameSuffixes(sourceFileSuffixes);
        this.sourceDirectories.add(sourceDirectory);
    }

    public void setSourceDirectories(List<File> directories) {
        for (File dir : directories) {
            SourceDirectory sourceDirectory = new SourceDirectory(dir, recursive);
            sourceDirectory.setFilenNameSuffixes(sourceFileSuffixes);
            this.sourceDirectories.add(sourceDirectory);
        }
    }

    public boolean isShouldMerge() {
        return shouldMerge;
    }

    public void setShouldMerge(boolean shouldMerge) {
        this.shouldMerge = shouldMerge;
    }

    public String getCdxSpec() {
        if (cdxSpec == null) {
            return CDXFormatIndex.CDX_HEADER_MAGIC;
        } else {
            return cdxSpec;
        }
    }

    public void setCdxSpec(String cdxSpec) {
        this.cdxSpec = cdxSpec;
    }

    public boolean isIdentity() {
        return identity;
    }

    public void setIdentity(boolean identity) {
        this.identity = identity;
    }

    public boolean isNewCanonClassic() {
        return newCanonClassic;
    }

    public void setNewCanonClassic(boolean newCanonClassic) {
        this.newCanonClassic = newCanonClassic;
    }

    public boolean isNewCanonSurt() {
        return newCanonSurt;
    }

    public void setNewCanonSurt(boolean newCanonSurt) {
        this.newCanonSurt = newCanonSurt;
    }

    public void verify() {
        if (outputDirectory == null) {
            throw new IllegalStateException("The output directory where cdx files for each of the arc/warc files are to be generated, must be set.");
        }

        if ((newCanonClassic && newCanonSurt)
                || (newCanonClassic && cdxSpec != null)
                || (newCanonSurt && cdxSpec != null)) {
            throw new IllegalStateException("Only one of newCanonClassic, newCanonSurt or cdxSpec can be set");
        }
    }

}
