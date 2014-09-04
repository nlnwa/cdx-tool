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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 *
 * @author John Erik Halse
 */
public class ResourceIndexHelper {

    private static final Logger log = Logger.getLogger(ResourceIndexHelper.class);

    private final File resourceIndexFile;

    private final SortedMap<String, File> db;

    private final Set<String> uniqueFileNames;

    public ResourceIndexHelper(File resourceIndexFile) {
        this.resourceIndexFile = resourceIndexFile.getAbsoluteFile();
        if (!resourceIndexFile.exists() && resourceIndexFile.getParentFile() != null) {
            resourceIndexFile.getParentFile().mkdirs();
        }

        db = new TreeMap<String, File>();
        uniqueFileNames = new HashSet<String>();
        readDB();
    }

    private void readDB() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(resourceIndexFile));
            try {
                String line = in.readLine();
                while (line != null) {
                    File file;
                    if (line.contains("\t")) {
                        file = new File(line.split("\t")[1]);
                    } else {
                        file = new File(line);
                    }
                    addFile(file);
                    line = in.readLine();
                }
            } catch (IOException ex) {
                log.fatal(ex.getMessage(), ex);
            }
        } catch (FileNotFoundException ex) {
            log.error("Resource index file '" + resourceIndexFile + "' not found");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    private File addFile(File file) {
        File canonicalFile;
        try {
            canonicalFile = file.getCanonicalFile();
            if (!uniqueFileNames.add(file.getName())) {
                log.debug("Filename: " + file.getName() + " already exist");
            }
            return db.put(canonicalFile.getName(), canonicalFile) == null ? canonicalFile : null;
        } catch (IOException ex) {
            log.fatal(ex.getMessage(), ex);
            return null;
        }
    }

    public synchronized void visited(File file) {
        File canonicalFile = addFile(file);
        if (canonicalFile != null) {
            writeDB();
        }
    }

    private synchronized void writeDB() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(resourceIndexFile, false));
            for (File f : db.values()) {
                out.write(f.getName() + "\t" + f.getPath());
                out.newLine();
            }
        } catch (IOException ex) {
            log.fatal(ex.getMessage(), ex);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public synchronized boolean isVisited(File entity) {
        try {
            return db.containsValue(entity.getCanonicalFile());
        } catch (IOException ex) {
            log.fatal(ex.getMessage(), ex);
            return false;
        }
    }
}
