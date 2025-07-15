package com.devonfw.tools.ide.merge.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;

public class TestWarning {
    
    @Test
    public void testWarning(@TempDir Path tempDir) throws Exception {
        // arrange
        IdeTestContext context = new IdeTestContext();
        EnvironmentVariables variables = context.getVariables();
        // Do NOT set FAIL_ON_AMBIGOUS_MERGE to true to get warning instead of error
        XmlMerger merger = new XmlMerger(context);
        Path folder = Path.of("src/test/resources/xmlmerger/ambiguous-id");
        Path sourcePath = folder.resolve("template.xml");
        Path targetPath = tempDir.resolve("target.xml");
        Files.copy(folder.resolve("target.xml"), targetPath);
        
        // act
        merger.merge(null, sourcePath, variables, targetPath);
        
        // This should produce a warning message that needs to be improved
    }
}