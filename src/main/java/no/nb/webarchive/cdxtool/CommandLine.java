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

import java.util.concurrent.ExecutionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Command line interface for CDX-tool.
 *
 * @author John Erik Halse
 */
public class CommandLine {

    private static final Logger log = Logger.getLogger(CommandLine.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try {
            Config config = parseArguments(new ArrayList<String>(Arrays.asList(args)));
            CdxTool cdxTool = new CdxTool();
            cdxTool.setConfig(config);
            cdxTool.execute(false);
        } catch (Exception ex) {
            log.fatal(ex.getMessage(), ex);
            usage();
        }
    }

    private static Config parseArguments(List<String> args) {
        Config config = new Config();

        while (!args.isEmpty() && args.get(0).startsWith("-")) {
            String currentArg = args.remove(0);
            switch (currentArg.charAt(1)) {
                case 'r':
                    config.setRecursive(true);
                    break;
                case 'c':
                    String mergedIndexFileName = getValue(currentArg, args);
                    config.setMergedIndexFile(new File(mergedIndexFileName));
                    break;
                case 'd':
                    String outDirName = getValue(currentArg, args);
                    config.setOutputDirectory(new File(outDirName));
                    break;
                case 's':
                    String scanInterval = getValue(currentArg, args);
                    config.setScanIntervalSeconds(Integer.parseInt(scanInterval));
                    break;
                case 'l':
                    String resourceIndexFileName = getValue(currentArg, args);
                    config.setResourceIndexFile(new File(resourceIndexFileName));
                    break;
                case 'f':
                    String cdxSpec = getValue(currentArg, args);
                    config.setCdxSpec(cdxSpec);
                    break;
                case 'm':
                    config.setShouldMerge(true);
                    break;
                case 'i':
                    config.setIdentity(true);
                    break;
                case 'n':
                    config.setNewCanonClassic(true);
                    break;
                case 'N':
                    config.setNewCanonSurt(true);
                    break;
                default:
                    System.out.println("Illegal argument: " + currentArg);
                    usage();
                    break;
            }
        }

        try {
            config.verify();
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
            log.trace(e.getMessage(), e);
            usage();
        }

        if (args.isEmpty()) {
            System.out.println("Arguments missing");
            usage();
        } else {
            while (!args.isEmpty()) {
                config.addSourceDirectory(new File(args.remove(0)));
            }
        }

        return config;
    }

    private static String getValue(String currentArgument, List<String> args) {
        String result = currentArgument.substring(2);
        if (result == null || result.isEmpty()) {
            result = args.remove(0);
        }
        return result;
    }

    public static void usage() {
        System.out.println("Tool for creating, sorting and merging CDX files for ARC/WARC files.");
        System.out.println("Usage: cdx-tool <options> <WARCDIR> [<WARCDIR> ...]\n");
        System.out.println("Options: ");
        System.out.println("  -r              Recursively scan subdirectories");
        System.out.println("  -c <CDXFILE>    Merged cdx file (default: ./index.cdx)");
        System.out.println("  -l <RESOURCEDB> Resource-location index file (default: ./path-index.txt)");
        System.out.println("  -d <WORKDIR>    Output directory where a cdx file for every arc/warc file is created (required)");
        System.out.println("  -s <SECONDS>    Scan interval in seconds, 0 = run once and quit (default: 0)");
        System.out.println("  -m              Merge cdx files into the file given by CDXFILE");
        System.out.println("  -i              Perform no url canonicalization");
        System.out.println("  -n              New url canonicalizator in classic mode");
        System.out.println("  -N              New url canonicalizator in surt mode");
        System.out.println("  -f <FORMAT>     Output CDX in format FORMAT");
        System.out.println("\nThe rest of the command line is interpreted as directories of arc/warc files.");
        System.out.println(" Both compressed and uncompressed files are supported.");
        System.exit(0);
    }

}
