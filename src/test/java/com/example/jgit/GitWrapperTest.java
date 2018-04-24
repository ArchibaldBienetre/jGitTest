package com.example.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class GitWrapperTest {

    private static final String MASTER = "master";
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
        GitWrapper.forLocalOnlyRepository(_tempDir);

        File expectedHiddenGitDir = new File(_tempDir, ".git");
        assertTrue(expectedHiddenGitDir.exists(), "should create hidden directory");
    }

    @Test
    public void test_that_GitWrapper_does_not_init_existing_repository() throws Exception {
        TestGitWrapper git1 = new TestGitWrapper(_tempDir);
        File expectedHiddenGitDir = new File(_tempDir, ".git");
        assertTrue(expectedHiddenGitDir.exists());
        assertTrue(git1.wasInitCalled());

        TestGitWrapper git2 = new TestGitWrapper(_tempDir);

        assertTrue(expectedHiddenGitDir.exists());
        assertFalse(git2.wasInitCalled());
    }

    @Test
    public void test_that_GitWrapper_can_commit_a_file_locally() throws Exception {
        assertTrue(new File(_tempDir, "blah.txt").createNewFile());
        String logMessage = getClass().getSimpleName() + ": committing a txt file";
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);

        sut.add("blah.txt");
        String sha1 = sut.commit(logMessage);

        assertEquals(sha1, sut.getLastLogSha1());
        assertEquals(logMessage, sut.getLastLogMessage());
        assertTrue(sut.getLastLogEntry().contains(sha1));
        assertTrue(sut.getLastLogEntry().contains(logMessage));
    }

    @Test
    public void test_that_GitWrapper_can_create_branch() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
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
    public void test_that_GitWrapper_can_switch_branches() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        assertFalse(sut.doesBranchExist(TEST_BRANCH));
        commitSomething(sut, "blah1.txt");
        String sha1Master = sut.getHeadSha1();
        sut.createBranchAndCheckout(TEST_BRANCH);
        String sha1Branch = commitSomething(sut, "blah2.txt");
        assertNotEquals(sha1Master, sha1Branch);
        assertEquals(sha1Branch, sut.getHeadSha1());

        String sha1Checkout = sut.checkOutBranch(MASTER);

        assertEquals(sha1Master, sha1Checkout);
    }

    @Test
    public void test_that_GitWrapper_can_delete_branch() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
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
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String sha1Master = commitSomething(sut, "blah1.txt");
        sut.createBranchAndCheckout(TEST_BRANCH);
        commitSomething(sut, "blah2.txt");
        assertEquals(TEST_BRANCH, sut.getCurrentBranchName());
        String sha1Branch = sut.getHeadSha1();
        assertNotEquals(sha1Master, sha1Branch);
        sut.checkOutBranch(MASTER);

        String actual = sut.merge(TEST_BRANCH);

        assertEquals(actual, sut.getHeadSha1());
        assertNotEquals(actual, sha1Branch);
        assertNotEquals(actual, sha1Master);
    }

    @Test
    public void test_getHeadSha1() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String sha1 = commitSomething(sut);
        assertEquals(MASTER, sut.getCurrentBranchName());
        String logSha1 = sut.getLastLogSha1();
        assertEquals(sha1, logSha1);

        String masterBranchTipSha1 = sut.getHeadSha1();

        assertEquals(masterBranchTipSha1, sha1);
    }

    @Test
    public void test_clean() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
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
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
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

    @Test
    public void test_resetHard() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String originalContent = "12345";
        File file = createNewFileWithContent("blah1.txt", originalContent);
        sut.add(".");
        String shaCommit1 = sut.commit("commit files");
        sut.clean();
        assertTrue(file.exists());
        assertFileContent(file, originalContent);
        String newContent = "overwritten";
        writeContentToFile(file, newContent);
        assertFileContent(file, newContent);

        String result = sut.resetHard();

        assertFileContent(file, originalContent);
        assertEquals(shaCommit1, sut.getHeadSha1());
        assertEquals(shaCommit1, result);
        assertIterableEquals(emptySet(), sut.clean());
    }

    @Test
    public void test_resetHardToRevisionSha1() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String content1 = "12345";
        String fileName = "blah1.txt";
        File file = createNewFileWithContent(fileName, content1);
        sut.add(".");
        String sha1Commit1 = sut.commit("commit files");
        assertFileContent(file, content1);

        String content2 = "98765";
        writeContentToFile(file, content2);
        sut.add(".");
        sut.commit("commit files 2");
        assertFileContent(file, content2);

        String uncommittedContent = "uncommitted";
        String uncommittedFileName = "test.txt";
        File uncommittedFile = createNewFileWithContent(uncommittedFileName, uncommittedContent);
        writeContentToFile(uncommittedFile, uncommittedContent);
        assertFileContent(uncommittedFile, uncommittedContent);

        String result = sut.resetHardTo(sha1Commit1);

        assertFileContent(file, content1);
        assertEquals(sha1Commit1, sut.getHeadSha1());
        assertEquals(sha1Commit1, result);
        // uncommitted files will still exist after this!
        assertTrue(uncommittedFile.exists());

    }

    @Test
    public void test_resetHardToBranch() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String content1 = "12345";
        String fileName = "blah1.txt";
        File file = createNewFileWithContent(fileName, content1);
        sut.add(".");
        String sha1Commit1 = sut.commit("commit files");
        assertFileContent(file, content1);
        assertEquals(MASTER, sut.getCurrentBranchName());

        sut.createBranchAndCheckout(TEST_BRANCH);
        String content2 = "98765";
        writeContentToFile(file, content2);
        sut.add(".");
        sut.commit("commit files 2");
        assertFileContent(file, content2);
        assertEquals(TEST_BRANCH, sut.getCurrentBranchName());

        String uncommittedContent = "uncommitted";
        String uncommittedFileName = "test.txt";
        File uncommittedFile = createNewFileWithContent(uncommittedFileName, uncommittedContent);
        writeContentToFile(uncommittedFile, uncommittedContent);
        assertFileContent(uncommittedFile, uncommittedContent);

        String result = sut.resetHardTo(MASTER);

        assertFileContent(file, content1);
        assertEquals(sha1Commit1, sut.getHeadSha1());
        assertEquals(sha1Commit1, result);
        // uncommitted files will still exist after this!
        assertTrue(uncommittedFile.exists());
    }

    @Test
    public void test_getFileContentOfRevision_can_retrieve_content_of_head_revision() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String expectedContent = "12345";
        String fileName = "blah1.txt";
        createNewFileWithContent(fileName, expectedContent);
        sut.add(".");
        sut.commit("commit files");

        Optional<String> optionalContent = sut.getFileContentOfRevision("HEAD", fileName);

        assertTrue(optionalContent.isPresent());
        assertEquals(expectedContent, optionalContent.get());
    }

    @Test
    public void test_getFileContentOfRevision_can_retrieve_content_of_old_revision() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String fileName = "blah1.txt";
        String content1 = "12345";
        String content2 = "987612345";
        File file = createNewFileWithContent(fileName, content1);
        sut.add(".");
        String sha1Commit1 = sut.commit("commit files (1)");
        writeContentToFile(file, content2);
        sut.add(".");
        String sha1Commit2 = sut.commit("commit files (2)");

        Optional<String> optionalContent1 = sut.getFileContentOfRevision(sha1Commit1, fileName);
        Optional<String> optionalContent2 = sut.getFileContentOfRevision(sha1Commit2, fileName);
        Optional<String> optionalContentNonPresentFile = sut.getFileContentOfRevision(sha1Commit2, fileName + ".nonexistent");

        assertTrue(optionalContent1.isPresent());
        assertEquals(content1, optionalContent1.get());
        assertTrue(optionalContent2.isPresent());
        assertEquals(content2, optionalContent2.get());
        assertFalse(optionalContentNonPresentFile.isPresent());
    }


    @Test
    public void test_lsTree_for_root_directory() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String file1Name = "blah1.txt";
        File file1 = createNewFileWithContent(file1Name, "12345");
        String dirName = "directory";
        File dir = new File(_tempDir, dirName);
        assertTrue(dir.mkdir());
        String file2Name = "blah2.txt";
        File file2 = new File(dir, file2Name);
        assertTrue(file2.createNewFile());
        sut.add(".");
        String commit1 = sut.commit("commit files");
        String file3Name = "blah3.txt";
        File file3 = new File(dir, file3Name);
        assertTrue(file3.createNewFile());
        sut.add(".");
        String commit2 = sut.commit("commit another file");
        sut.clean();
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());

        List<String> actualOutputCommit1 = sut.lsTree(commit1, ".");
        List<String> actualOutputCommit2 = sut.lsTree(commit2, ".");

        assertFalse(actualOutputCommit1.isEmpty());
        assertEquals(2, actualOutputCommit1.size());
        assertTrue(actualOutputCommit1.contains(file1Name));
        assertTrue(actualOutputCommit1.contains(dirName + "/" + file2Name));
        assertFalse(actualOutputCommit1.contains(dirName + "/" + file3Name));
        assertFalse(actualOutputCommit2.isEmpty());
        assertEquals(3, actualOutputCommit2.size());
        assertTrue(actualOutputCommit2.contains(file1Name));
        assertTrue(actualOutputCommit2.contains(dirName + "/" + file2Name));
        assertTrue(actualOutputCommit2.contains(dirName + "/" + file3Name));
    }

    @Test
    public void test_lsTree_for_specific_directory() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        File file1 = createNewFileWithContent("blah1.txt", "12345");
        String dirName = "directory";
        File dir = new File(_tempDir, dirName);
        assertTrue(dir.mkdir());
        String file2Name = "blah2.txt";
        File file2 = new File(dir, file2Name);
        assertTrue(file2.createNewFile());
        sut.add(".");
        String commit1 = sut.commit("commit files");
        String file3Name = "blah3.txt";
        File file3 = new File(dir, file3Name);
        assertTrue(file3.createNewFile());
        sut.add(".");
        String commit2 = sut.commit("commit another file");
        sut.clean();
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());

        List<String> actualOutputCommit1 = sut.lsTree(commit1, dirName);
        List<String> actualOutputCommit2 = sut.lsTree(commit2, dirName);

        assertFalse(actualOutputCommit1.isEmpty());
        assertEquals(1, actualOutputCommit1.size());
        assertTrue(actualOutputCommit1.contains(dirName + "/" + file2Name));
        assertFalse(actualOutputCommit2.isEmpty());
        assertEquals(2, actualOutputCommit2.size());
        assertTrue(actualOutputCommit2.contains(dirName + "/" + file2Name));
        assertTrue(actualOutputCommit2.contains(dirName + "/" + file3Name));
    }

    @Test
    public void test_getCommitsBetween() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String commit1 = commitSomething(sut, "blah1.txt");
        String commit2 = commitSomething(sut, "blah2.txt");
        String commit3 = commitSomething(sut, "blah3.txt");
        String commit4 = commitSomething(sut, "blah4.txt");

        List<String> actualAll = sut.getCommitsBetween(commit1, commit4);
        List<String> actualAll2 = sut.getCommitsBetween(commit1, "HEAD");
        List<String> actualFirstMissing = sut.getCommitsBetween(commit2, commit4);
        List<String> actualLastMissing = sut.getCommitsBetween(commit1, commit3);
        List<String> actualSameCommit = sut.getCommitsBetween(commit4, commit4);
        List<String> actualSameCommit2 = sut.getCommitsBetween(commit4, "HEAD");
        List<String> actualSameCommit3 = sut.getCommitsBetween(commit3, commit3);

        List<String> expectedAll = asList(commit3, commit2);
        assertEquals(actualAll, actualAll2);
        assertEquals(expectedAll, actualAll);
        List<String> expectedFirstMissing = singletonList(commit3);
        assertEquals(expectedFirstMissing, actualFirstMissing);
        List<String> expectedLastMissing = singletonList(commit2);
        assertEquals(expectedLastMissing, actualLastMissing);
        List<Object> expectedSameCommit = Collections.emptyList();
        assertEquals(actualSameCommit, actualSameCommit2);
        assertEquals(actualSameCommit2, actualSameCommit3);
        assertEquals(expectedSameCommit, actualSameCommit);
    }

    @Test
    public void test_getMergeBase() throws Exception {
        GitWrapper sut = GitWrapper.forLocalOnlyRepository(_tempDir);
        String baseCommit = commitSomething(sut, "blah1.txt");
        sut.createBranchAndCheckout(TEST_BRANCH);
        String commitOnBranch = commitSomething(sut, "blah2.txt");
        sut.checkOutBranch(MASTER);
        String commitOnMaster = commitSomething(sut, "blah3.txt");

        Optional<String> actual1 = sut.getMergeBase(commitOnMaster, commitOnBranch);
        Optional<String> actual2 = sut.getMergeBase(MASTER, TEST_BRANCH);

        assertTrue(actual1.isPresent());
        assertEquals(baseCommit, actual1.get());
        assertTrue(actual2.isPresent());
        assertEquals(baseCommit, actual2.get());
    }

    private void assertFileContent(File file, String expected) throws IOException {
        String actual = new BufferedReader(new FileReader(file)).readLine();
        assertEquals(expected, actual);
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
        writeContentToFile(file, content);
        return file;
    }

    private void writeContentToFile(File file, String content) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(content);
        fileWriter.flush();
    }

    private File createNewFile(String fileName) throws IOException {
        File file = new File(_tempDir, fileName);
        assertTrue(file.createNewFile());
        return file;
    }

    private static class TestGitWrapper extends GitWrapper {
        private boolean _initCalled;

        TestGitWrapper(File directory) throws IOException, GitAPIException {
            super(directory);
        }

        @Override
        void init(File directory) throws GitAPIException {
            _initCalled = true;
            super.init(directory);
        }

        boolean wasInitCalled() {
            return _initCalled;
        }
    }

}