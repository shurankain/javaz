package uml;

import org.javaz.uml.RenderFtl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RenderFtlTest {

    private String tmp = System.getProperty("java.io.tmpdir");

    private List<String> createdFiles = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
        copyFile("test.include.child.ftl", tmp + "1" + File.separatorChar + "test.include.child.ftl");
        copyFile("test.include.parent.ftl", tmp + "2" + File.separatorChar + "test.include.parent.ftl");
        copyFile("test.include.parent-name.ftl", tmp + "2" + File.separatorChar + "test.include.parent-name.ftl");
    }

    @After
    public void tearDown() throws Exception {
        for (String createdFile : createdFiles) {
            File file = new File(createdFile);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    public void testInclude() throws Exception {
        RenderFtl renderFtl = new RenderFtl(new HashMap());
        renderFtl.setTemplatePath(tmp);
        renderFtl.setTemplate("2\\test.include.parent");
        String outPath = tmp + "out";
        renderFtl.setOutPath(outPath);
        renderFtl.renderTemplate();
        File outFile = new File(outPath + "/SomeName");
        byte[] bytes = Files.readAllBytes(outFile.toPath());
        String result = new String(bytes);
        createdFiles.add(outFile.getCanonicalPath());
        Assert.assertEquals("some text", result);
    }

    private void copyFile(String in, String out) throws IOException {
        InputStream inputStream = RenderFtlTest.class.getClassLoader().getResourceAsStream(in);
        File file = new File(out);
        file.getParentFile().mkdirs();
        int readBytes;
        byte[] buffer = new byte[4096];
        OutputStream outputStream = new FileOutputStream(file);
        while ((readBytes = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, readBytes);
        }
        inputStream.close();
        outputStream.close();
        createdFiles.add(out);
    }
}