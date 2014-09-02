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
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author John Erik Halse
 */
public class SourceDirectory {

    private static final Logger log = Logger.getLogger(SourceDirectory.class.getName());

    private final File directory;

    private boolean recursive;

    private String[] fileNameSuffixes;

    public SourceDirectory(File directory) {
        this.directory = directory;
        this.recursive = false;
    }

    public SourceDirectory(File directory, boolean recursive) {
        this.directory = directory;
        this.recursive = recursive;
    }

    public File getDirectory() {
        return directory;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String[] getFilenNameSuffixes() {
        return fileNameSuffixes;
    }

    public void setFilenNameSuffixes(String... fileNameSuffixes) {
        this.fileNameSuffixes = fileNameSuffixes;
    }

    public void walk(FileVisitor visitor) {
        FileFilter filter = new Filter(fileNameSuffixes);
        File[] baseDirFiles = directory.listFiles(filter);
        innerWalk(baseDirFiles, visitor, filter);
    }

    private void innerWalk(File[] files, FileVisitor visitor, FileFilter filter) {
        for (File file : files) {
            if (file.isDirectory()) {
                innerWalk(file.listFiles(filter), visitor, filter);
            } else {
                visitor.visit(file);
            }
        }
    }

    private class Filter implements FileFilter {

        private final String[] fileNameSuffixes;

        public Filter(String[] fileNameSuffixes) {
            this.fileNameSuffixes = fileNameSuffixes;
        }

        @Override
        public boolean accept(File pathname) {
            boolean accepted = false;

            if (recursive && pathname.isDirectory()) {
                accepted = true;
            } else if (pathname.isFile()) {
                if (fileNameSuffixes == null) {
                    accepted = true;
                } else {
                    for (String suffix : fileNameSuffixes) {
                        if (pathname.getName().endsWith(suffix)) {
                            accepted = true;
                            break;
                        }
                    }
                }
            }

            if (!pathname.canRead()) {
                log.log(Level.WARNING, "Cannot read {0}", pathname.getAbsolutePath());
                accepted = false;
            }

            return accepted;
        }

    }

    @Override
    public String toString() {
        return directory.getAbsolutePath();
    }

}
