package com.example.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Iterables.getOnlyElement;

/**
 * Encapsulates a GIT repository in the file system using <a href="http://wiki.eclipse.org/JGit/User_Guide">jGit</a>
 */
public class GitWrapper {

    private final Git _git;

    /**
     * Create or open a GIT repository at the given directory
     */
    public GitWrapper(File directory) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setWorkTree(directory);
        File hiddenGitDir = builder.getGitDir();
        if (hiddenGitDir == null || !hiddenGitDir.exists()) {
            Git.init().setBare(false).setDirectory(directory).call();
        }
        Repository repository = builder.build();
        _git = new Git(repository);
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-add">git add</a>
     * @param filePattern a concrete file name, or "." for all files - <em>jGit's add does not support globs (like, *.java), as of yet</em>
     */
    public void add(String filePattern) throws GitAPIException {
        _git.add().addFilepattern(filePattern).call();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-clean">git clean -dfx</a>
     *
     * @return a list of cleaned files
     */
    public Set<String> clean() throws GitAPIException {
        return _git.clean()
                .setCleanDirectories(true)
                .setForce(true)
                .setIgnore(false)
                .call();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-commit">git commit -m</a>
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
        RevCommit logEntry = getOnlyElement(_git.log().setMaxCount(1).call());
        return logEntry.getId().toString() + ": " + logEntry.getShortMessage();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's SHA-1
     */
    public String getLastLogSha1() throws GitAPIException {
        RevCommit logEntry = getOnlyElement(_git.log().setMaxCount(1).call());
        return ObjectId.toString(logEntry);
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-log">git-log</a>
     *
     * @return successful commit's short message
     */
    public String getLastLogMessage() throws GitAPIException {
        RevCommit logEntry = getOnlyElement(_git.log().setMaxCount(1).call());
        return logEntry.getShortMessage();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-checkout">git checkout -b</a>
     */
    public String createBranchAndCheckout(String branchName) throws GitAPIException {
        Ref ref = _git.checkout().setCreateBranch(true).setName(branchName).call();
        return ObjectId.toString(ref.getObjectId());
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-checkout">git checkout</a>
     */
    public String checkOutBranch(String branchName) throws GitAPIException {
        Ref ref = _git.checkout().setName(branchName).call();
        return ObjectId.toString(ref.getObjectId());
    }

    /**
     * Checks out branch ({@link #checkOutBranch(String)}), and
     * encapsulates <a href="https://git-scm.com/docs/git-branch">git branch -D</a>
     */
    public String checkoutMasterAndDeleteBranch(String branchName) throws GitAPIException {
        checkOutBranch("master");
        return getOnlyElement(_git.branchDelete().setForce(true).setBranchNames(branchName).call());
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-merge">git-merge --no-ff</a>
     */
    public String merge(String branchName) throws GitAPIException {
        Optional<Ref> branchWithMatchingName = findBranchByName(branchName);
        Ref aCommit = branchWithMatchingName.get();
        MergeResult mergeResult = _git.merge()
                .include(aCommit)
                .setCommit(true) // no dry run
                .setFastForward(MergeCommand.FastForwardMode.NO_FF) // create a merge commit
                .call();
        return ObjectId.toString(mergeResult.getNewHead());
    }

    public String getHeadSha1() throws IOException {
        return ObjectId.toString(_git.getRepository().resolve("HEAD"));
    }

    public String getCurrentBranchName() throws IOException {
        return _git.getRepository().getBranch();
    }

    public boolean doesBranchExist(String branchName) throws GitAPIException {
        return findBranchByName(branchName).isPresent();
    }

    private Optional<Ref> findBranchByName(String branchName) throws GitAPIException {
        List<Ref> branches = _git.branchList().call();
        return branches.stream()
                .filter(branch -> branch.getName().equals("refs/heads/" + branchName))
                .findFirst();
    }
}
