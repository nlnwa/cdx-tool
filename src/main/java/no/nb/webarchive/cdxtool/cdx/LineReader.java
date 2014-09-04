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
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * @author John Erik Halse
 */
public class LineReader {

    private static final Logger log = Logger.getLogger(LineReader.class);

    final BufferedReader in;

    String currentLine;

    private boolean eof = false;

    public LineReader(BufferedReader in) throws IOException {
        this.in = in;
        init();
    }

    public LineReader(File file) throws IOException {
        this(new BufferedReader(new FileReader(file), 1024 * 768));
    }

    void init() throws IOException {
        readLine();
    }
    
    void readLine() throws IOException {
        if (!eof) {
            currentLine = in.readLine();
            if (currentLine == null) {
                eof = true;
                try {
                    in.close();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    public String next() throws IOException {
        String result = currentLine;
        readLine();
        return result;
    }

    public boolean hasMore() {
        return currentLine != null;
    }

    public boolean isGreatherThan(LineReader other) throws IOException {
        if (other == null || other.currentLine == null) {
            return false;
        }

        if (currentLine == null) {
            return true;
        }

        int comp = currentLine.compareTo(other.currentLine);
        if (comp > 0) {
            return true;
        } else if (comp == 0) {
            other.readLine();
        }
        return false;
    }

    @Override
    public String toString() {
        return currentLine;
    }
}
