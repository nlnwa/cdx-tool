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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author John Erik Halse
 */
public class MergerTest {
    
    @Test
    public void testMerger() throws IOException {
        String expectedResult = "a\nb\nc\nh\nj\nk\nl\no\nr\nx\ny\nz\nå\nr\no\nr\nå\næ\n";
        StringWriter result = new StringWriter();
        File inDir = new File("target/test-classes/mergerdata");
        Merger merger = new Merger(inDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cdx");
            }
        }), new PrintWriter(result), false);

        assertEquals(expectedResult, result.toString());
    }
    
    @Test
    public void testMergerWithSorting() throws IOException {
        String expectedResult = "a\nb\nc\nh\nj\nk\nl\no\nr\nx\ny\nz\nå\næ\n";
        StringWriter result = new StringWriter();
        File inDir = new File("target/test-classes/mergerdata");
        Merger merger = new Merger(inDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cdx");
            }
        }), new PrintWriter(result), true);

        assertEquals(expectedResult, result.toString());
    }
}
