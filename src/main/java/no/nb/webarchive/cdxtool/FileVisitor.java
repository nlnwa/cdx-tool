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

/**
 *
 * @author John Erik Halse
 */
public interface FileVisitor {
    /**
     * Process the file
     *
     * @param file
     */
    public void visit(File file);

    /**
     * Task to be run after all files are visited
     *
     */
    public void postProcess();

    /**
     * Task to be run before visiting files
     *
     */
    public void preProcess();
}
