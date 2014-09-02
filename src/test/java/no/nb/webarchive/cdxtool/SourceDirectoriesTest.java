/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.nb.webarchive.cdxtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import no.nb.webarchive.cdxtool.cdxjob.CdxGeneratorVisitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author johnh
 */
public class SourceDirectoriesTest {
    private static final File RESOURCE_LOCATION_FILE = new File("target/test-classes/path-index.txt");

    File dbDir = new File("target/testdb");

    File outDir = new File("target/testOut");

    File growingFile = new File(dbDir, "growing.arc");

    Future writerFuture;

    public SourceDirectoriesTest() {
    }

    @Before
    public void setUp() throws Exception {
        deleteDir(dbDir);
        dbDir.mkdirs();
        deleteDir(outDir);
        outDir.mkdirs();
        writerFuture = Executors.newSingleThreadExecutor().submit(new FileWriter());
    }

    @After
    public void tearDown() throws Exception {
        writerFuture.get();
    }

    /**
     * Test of walk method, of class SourceDirectories.
     */
    @Test
    public void testWalk() throws InterruptedException, IOException {
        System.out.println("walk");
        SourceDirectories sourceDirs = new SourceDirectories();

        boolean recursive = true;

        sourceDirs.addSourceDirectory(new SourceDirectory(new File("src/test/resources"), recursive));
        sourceDirs.addSourceDirectory(new SourceDirectory(dbDir, recursive));

        Config config = new Config();
        config.setMergedIndexFile(new File(dbDir, "index.cdx"));
        config.setResourceIndexFile(RESOURCE_LOCATION_FILE);
        config.setOutputDirectory(outDir);

        CdxGeneratorVisitor cdxJob = new CdxGeneratorVisitor(config);

        sourceDirs.walk(cdxJob);
        cdxJob.postProcess();

        assertEquals(3, cdxJob.getFileCount());
        assertEquals(3145191, cdxJob.getTotalSize());
    }

    private class FileWriter implements Runnable {

        @Override
        public void run() {
            try {
                FileInputStream in = new FileInputStream("src/test/resources/test2.arc.gz");
                FileOutputStream out = new FileOutputStream(growingFile, true);
                
                byte[] buf = new byte[1024 * 8];
                int len = in.read(buf);
                while (len > -1) {
                    out.write(buf, 0, len);
                    len = in.read(buf);
                }
                in.close();
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
            System.out.println("Done writing");
        }
    }

    private void deleteDir(File dir) {
        System.out.println("Deleting " + dir);
        if (dir.exists()) {
            try {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
                dir.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
