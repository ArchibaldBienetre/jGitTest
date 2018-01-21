package com.example.jgit;

import com.google.common.collect.Iterables;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Encapsulates a GIT repository in the file system using <a href="http://wiki.eclipse.org/JGit/User_Guide">jGit</a>
 */
public class GitWrapper {

    private final Git _git;

    /**
     * Create or open a GIT repository at the given directory
     */
    public GitWrapper(File directory) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.setWorkTree(directory);
//                .readEnvironment() // scan environment GIT_* variables
//                .findGitDir(); // scan up the file system tree
        File hiddenGitDir = builder.getGitDir();
        if (hiddenGitDir == null || !hiddenGitDir.exists()) {
            Git.init().setBare(false).setDirectory(directory).call();
        }
        Repository repository = builder.build();
        _git = new Git(repository);
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-add">git-add</a>
     */
    public void add(String filePattern) throws GitAPIException {
        _git.add().addFilepattern(filePattern).call();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-commit">git-commit</a>
     *
     * @return successful commit's SHA-1
     */
    public String commit(String message) throws GitAPIException {
        RevCommit revision = _git.commit().setMessage(message).call();
        return ObjectId.toString(revision);
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's SHA-1, followed by the commit's short message
     */
    public String getLastLogEntry() throws GitAPIException {
        RevCommit logEntry = Iterables.getOnlyElement(_git.log().setMaxCount(1).call());
        return logEntry.getId().toString() + ": " + logEntry.getShortMessage();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's SHA-1
     */
    public String getLastLogSha1() throws GitAPIException {
        RevCommit logEntry = Iterables.getOnlyElement(_git.log().setMaxCount(1).call());
        return ObjectId.toString(logEntry);
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's short message
     */
    public String getLastLogMessage() throws GitAPIException {
        RevCommit logEntry = Iterables.getOnlyElement(_git.log().setMaxCount(1).call());
        return logEntry.getShortMessage();
    }
}