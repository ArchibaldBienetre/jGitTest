package com.example.jgit;

import org.eclipse.jgit.util.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitWrapperTest {

    private static File _tempDir;

    @BeforeAll
    public static void setUp() throws IOException {
        _tempDir = Files.createTempDirectory(GitWrapperTest.class.getSimpleName()).toFile();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        FileUtils.delete(_tempDir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS);
    }

    @Test
    public void test_that_GitWrapper_creates_a_repository_at_given_folder() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);

        File expectedHiddenGitDir = new File(_tempDir, ".git");
        assertTrue(expectedHiddenGitDir.exists(), "should create hidden directory");
    }
}