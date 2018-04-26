package com.example.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ThrowingGitWrapper {
    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-add">git add</a> for a specific file pattern.
     *
     * @param filePattern a concrete file name, or "." for all non-deleted files -
     *                    <em>jGit's add does not support globs (like, *.java), as of yet, and will behave weirdly with deleted files</em>
     */
    void addFileIfNotDeleted(String filePattern) throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-add">git add</a> for the current directory.
     * <em>jGit's add behaves weirdly when it comes to deleted files - best use {@link #addRemovedFile(String)} ddRemovedFile()} to delete files!</em>
     */
    void addAllExceptDeletedFiles() throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-rm">git rm</a> for the current directory.
     *
     * @param filePattern see {@link #addFileIfNotDeleted(String)}
     */
    void addRemovedFile(String filePattern) throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-clean">git clean -dfx</a>
     *
     * @return a list of cleaned files
     */
    Set<String> clean() throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-commit">git commit -m</a>
     *
     * @return successful commit's SHA-1
     */
    String commit(String message) throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's SHA-1, followed by the commit's short message
     */
    String getLastLogEntry() throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's SHA-1
     */
    String getLastLogSha1() throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's short message
     */
    String getLastLogMessage() throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-checkout">git checkout -b</a>
     *
     * @return SHA-1 of HEAD after checkout (still equal to the one you branched from)
     */
    String createBranchAndCheckout(String branchName) throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-checkout">git checkout</a>
     *
     * @return SHA-1 of HEAD after checkout
     */
    String checkOutBranch(String branchName) throws GitAPIException;

    /**
     * Checks out branch ({@link #checkOutBranch(String)}), and
     * encapsulates <a href="https://git-scm.com/docs/git-branch">git branch -D</a>
     *
     * @return name of deleted branch as ref ("refs/heads/BRANCH")
     */
    String checkoutMasterAndDeleteBranch(String branchName) throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-merge">git-merge --no-ff</a>
     *
     * @return SHA-1 of HEAD after merge (merge commit)
     */
    String merge(String branchName) throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-reset">git reset --hard</a>
     *
     * @return SHA-1 of HEAD after reset
     */
    String resetHard() throws GitAPIException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-reset">git reset --hard</a>
     *
     * @return SHA-1 of HEAD after reset
     */
    String resetHardTo(String sha1OrBranch) throws GitAPIException;

    String getHeadSha1() throws IOException;

    String getCurrentBranchName() throws IOException;

    /**
     * Retrieve the content of a file in a given revision.
     * <p>
     * Shoutouts: could not have guessed how to do it without
     * <a href="https://stackoverflow.com/questions/39696689/jgit-use-treewalk-to-get-content-of-file">the folks on Stackoverflow</a>.
     *
     * @param revisionString revision String identifying the revision you want
     * @param filePath       path to file you want to retrieve
     * @return the content of the file if found (first match only)
     */
    Optional<String> getFileContentOfRevision(String revisionString, String filePath) throws IOException;

    /**
     * Retrieve the content of a directory in a given revision.
     * <p>
     * Shoutouts: could not have guessed how to do it without
     * <a href="https://stackoverflow.com/questions/39696689/jgit-use-treewalk-to-get-content-of-file">the folks on Stackoverflow</a>.
     *
     * @param revisionString revision String identifying the revision you want
     * @param directoryPath  path to directory you want to retrieve
     * @return the content of the file if found (first match only)
     */
    List<String> lsTree(String revisionString, String directoryPath) throws IOException;

    /**
     * Encapsulates a simple <a href="https://git-scm.com/docs/git-rev-list">git rev-list olderRevision youngerRevision</a>
     *
     * @return List of SHA-1 of commits between the given ones
     */
    List<String> getCommitsBetween(String olderExclusive, String youngerExclusive) throws IOException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-merge-base">git merge-base revision1 revision2</a>
     *
     * @return Optional SHA-1 of merge-base commit if it exists
     */
    Optional<String> getMergeBase(String revisionString1, String revisionString2) throws IOException;

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-diff">git diff</a>
     * <p>
     * Returns a mapping file path > {@link GitDiffType} containing the differences between the given revisions.
     * <p>
     */
    Map<String, GitDiffType> getFileToDiffTypeForRevision(String revisionStringOld, String revisionStringNew) throws IOException;

    /**
     * @param recognizeRenames if true, moved and renamed files will be recognized as {@link GitDiffType#RENAME} (instead of separate DELETE and ADD)
     * @see #getFileToDiffTypeForRevision(String, String)
     */
    Map<String, GitDiffType> getFileToDiffTypeForRevision(String revisionStringOld, String revisionStringNew, boolean recognizeRenames) throws IOException;

    boolean doesBranchExist(String branchName) throws GitAPIException;
}
