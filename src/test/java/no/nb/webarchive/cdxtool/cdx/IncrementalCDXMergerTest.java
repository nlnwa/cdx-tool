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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author John Erik Halse
 */
public class IncrementalCDXMergerTest {

    private static final File MERGED_CDX_FILE = new File("target/test-classes/index.cdx");
    private static final File INPUT_CDX_DIR = new File("target/test-classes/mergerdata");

    public IncrementalCDXMergerTest() {
    }

    @Test
    public void testSplitFileArray() {
        IncrementalCDXMerger cdxMerger = new IncrementalCDXMerger(INPUT_CDX_DIR, MERGED_CDX_FILE, 2);
        File[][] splittedFiles = cdxMerger.
                splitFileArray(cdxMerger.findFiles());
        assertEquals(3, splittedFiles.length);
        assertEquals(1, splittedFiles[splittedFiles.length - 1].length);

        cdxMerger = new IncrementalCDXMerger(INPUT_CDX_DIR, MERGED_CDX_FILE, 3);
        splittedFiles = cdxMerger.splitFileArray(cdxMerger.findFiles());
        assertEquals(2, splittedFiles.length);
        assertEquals(2, splittedFiles[splittedFiles.length - 1].length);

        cdxMerger = new IncrementalCDXMerger(INPUT_CDX_DIR, MERGED_CDX_FILE, 4);
        splittedFiles = cdxMerger.splitFileArray(cdxMerger.findFiles());
        assertEquals(2, splittedFiles.length);
        assertEquals(1, splittedFiles[splittedFiles.length - 1].length);

        cdxMerger = new IncrementalCDXMerger(INPUT_CDX_DIR, MERGED_CDX_FILE, 5);
        splittedFiles = cdxMerger.splitFileArray(cdxMerger.findFiles());
        assertEquals(1, splittedFiles.length);
        assertEquals(5, splittedFiles[splittedFiles.length - 1].length);
    }

    @Test
    public void testExecute() throws IOException {
        String expectedResult = "a\nb\nc\nh\nj\nk\nl\no\nr\nx\ny\nz\nå\næ\n";
        StringBuilder result = new StringBuilder();

        IncrementalCDXMerger cdxMerger = new IncrementalCDXMerger(INPUT_CDX_DIR, MERGED_CDX_FILE, 2);
        cdxMerger.execute();

        BufferedReader in = new BufferedReader(new FileReader(MERGED_CDX_FILE));
        String line = in.readLine();
        while (line != null) {
            result.append(line).append("\n");
            line = in.readLine();
        }
        in.close();

        assertEquals(expectedResult, result.toString());
    }

}
