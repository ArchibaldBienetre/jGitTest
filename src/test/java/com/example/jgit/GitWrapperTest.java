package com.example.jgit;

import org.eclipse.jgit.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;

public class GitWrapperTest {

    private static final String TEST_BRANCH = "TEST_Branch";
    private File _tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        _tempDir = Files.createTempDirectory(GitWrapperTest.class.getSimpleName()).toFile();
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.delete(_tempDir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS);
    }

    @Test
    public void test_that_GitWrapper_creates_a_repository_at_given_folder() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);

        File expectedHiddenGitDir = new File(_tempDir, ".git");
        assertTrue(expectedHiddenGitDir.exists(), "should create hidden directory");
    }

    @Test
    public void test_that_GitWrapper_can_commit_a_file_locally() throws Exception {
        assertTrue(new File(_tempDir, "blah.txt").createNewFile());
        String logMessage = getClass().getSimpleName() + ": committing a txt file";
        GitWrapper sut = new GitWrapper(_tempDir);

        sut.add("blah.txt");
        String sha1 = sut.commit(logMessage);

        assertEquals(sha1, sut.getLastLogSha1());
        assertEquals(logMessage, sut.getLastLogMessage());
        assertTrue(sut.getLastLogEntry().contains(sha1));
        assertTrue(sut.getLastLogEntry().contains(logMessage));
    }

    @Test
    public void test_that_GitWrapper_can_create_branch() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);
        assertFalse(sut.doesBranchExist(TEST_BRANCH));
        commitSomething(sut, "blah1.txt");
        String sha1Master = sut.getHeadSha1();

        String sha1CreateBranch = sut.createBranchAndCheckout(TEST_BRANCH);
        commitSomething(sut, "blah2.txt");

        assertEquals(sha1CreateBranch, sha1Master);
        assertTrue(sut.doesBranchExist(TEST_BRANCH));
        String sha1BranchHead = sut.getHeadSha1();
        assertNotEquals(sha1Master, sha1BranchHead);
    }

    @Test
    public void test_that_GitWrapper_can_delete_branch() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);
        commitSomething(sut);
        String sha1Master = sut.getHeadSha1();
        sut.createBranchAndCheckout(TEST_BRANCH);
        assertTrue(sut.doesBranchExist(TEST_BRANCH));

        String actual = sut.checkoutMasterAndDeleteBranch(TEST_BRANCH);

        assertFalse(sut.doesBranchExist(TEST_BRANCH));
        assertEquals("refs/heads/" + TEST_BRANCH, actual);
        assertEquals(sha1Master, sut.getHeadSha1());
    }

    @Test
    public void test_merge() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);
        String sha1Master = commitSomething(sut, "blah1.txt");
        sut.createBranchAndCheckout(TEST_BRANCH);
        commitSomething(sut, "blah2.txt");
        assertEquals(TEST_BRANCH, sut.getCurrentBranchName());
        String sha1Branch = sut.getHeadSha1();
        assertNotEquals(sha1Master, sha1Branch);
        sut.checkOutBranch("master");

        String actual = sut.merge(TEST_BRANCH);

        assertNotEquals(actual, sha1Branch);
        assertNotEquals(actual, sha1Master);
    }

    @Test
    public void test_getHeadSha1() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);
        String sha1 = commitSomething(sut);
        assertEquals("master", sut.getCurrentBranchName());
        String logSha1 = sut.getLastLogSha1();
        assertEquals(sha1, logSha1);

        String masterBranchTipSha1 = sut.getHeadSha1();

        assertEquals(masterBranchTipSha1, sha1);
    }

    @Test
    public void test_clean() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);
        File committedFile = createNewFileWithContent("blah1.txt", "1234");
        String logMessage = getClass().getSimpleName() + ": committing a txt file";
        sut.add("blah1.txt");
        sut.commit(logMessage);
        File unversionedFile = createNewFileWithContent("blah2.txt", "98765");
        assertTrue(committedFile.exists());
        assertTrue(unversionedFile.exists());

        Set<String> result = sut.clean();

        assertTrue(committedFile.exists());
        assertFalse(unversionedFile.exists());
        assertIterableEquals(singleton("blah2.txt"), result);
    }

    @Test
    public void test_that_add_may_add_whole_directory() throws Exception {
        GitWrapper sut = new GitWrapper(_tempDir);
        File file1 = createNewFileWithContent("blah1.txt", "12345");
        File file2 = createNewFileWithContent("blah2.txt", "123456");
        File dir = new File(_tempDir, "directory");
        assertTrue(dir.mkdir());
        File file3 = new File(dir, "blah3.txt");
        assertTrue(file3.createNewFile());
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());

        sut.add(".");
        sut.commit("commit files");
        sut.clean();

        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    private String commitSomething(GitWrapper sut) throws Exception {
        return commitSomething(sut, "blah.txt");
    }

    private String commitSomething(GitWrapper sut, String fileName) throws Exception {
        createNewFile(fileName);
        String logMessage = getClass().getSimpleName() + ": committing a txt file";
        sut.add(fileName);
        return sut.commit(logMessage);
    }

    private File createNewFileWithContent(String fileName, String content) throws IOException {
        File file = createNewFile(fileName);
        (new FileWriter(file)).write(content);
        return file;
    }

    private File createNewFile(String fileName) throws IOException {
        File file = new File(_tempDir, fileName);
        assertTrue(file.createNewFile());
        return file;
    }
}