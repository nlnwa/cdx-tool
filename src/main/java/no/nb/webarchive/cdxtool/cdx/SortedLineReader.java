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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author John Erik Halse
 */
public class SortedLineReader extends LineReader {

    private static final Logger log = Logger.getLogger(SortedLineReader.class.getName());

    private SortedSet<String> sortedLineSet;

    public SortedLineReader(BufferedReader in) throws IOException {
        super(in);
    }

    public SortedLineReader(File file) throws IOException {
        super(file);
    }

    @Override
    void init() throws IOException {
        sortedLineSet = new TreeSet<String>();
        String line = in.readLine();
        while (line != null) {
            sortedLineSet.add(line);
            line = in.readLine();
        }

        try {
            in.close();
        } catch (IOException ex) {
            log.log(Level.WARNING, ex.getMessage(), ex);
        }

        readLine();
    }

    @Override
    void readLine() throws IOException {
        if (sortedLineSet.isEmpty()) {
            currentLine = null;
        } else {
            currentLine = sortedLineSet.first();
            sortedLineSet.remove(currentLine);
        }
    }
}
