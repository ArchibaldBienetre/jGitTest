package com.example.jgit;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Iterables.getOnlyElement;

/**
 * Encapsulates a GIT repository in the file system using <a href="http://wiki.eclipse.org/JGit/User_Guide">jGit</a>
 */
public class GitWrapper {

    /**
     * Create or open a GIT repository at the given directory
     */
    public static GitWrapper forLocalOnlyRepository(File directory) throws IOException, GitAPIException {
        return new GitWrapper(directory);
    }

    private final Git _git;

    @VisibleForTesting
    GitWrapper(File directory) throws IOException, GitAPIException {
        _git = localSetup(directory);
    }

    private Git localSetup(File directory) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setWorkTree(directory);
        File hiddenGitDir = new File(directory, ".git");
        if (!hiddenGitDir.exists()) {
            // Calling git init on an existing repository should not do much harm  -
            // BUT but own hook skripts that are not part of the init template may be replaced.
            // We don't want that - plus, init att this point would be unexpected.
            init(directory);
        }
        Repository repository = builder.build();
        return new Git(repository);
    }

    @VisibleForTesting
    void init(File directory) throws GitAPIException {
        // no bare repository - this is the client part
        Git.init().setBare(false).setDirectory(directory).call();
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-add">git add</a>
     *
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
     *
     * @return SHA-1 of HEAD after checkout (still equal to the one you branched from)
     */
    public String createBranchAndCheckout(String branchName) throws GitAPIException {
        Ref ref = _git.checkout().setCreateBranch(true).setName(branchName).call();
        return ObjectId.toString(ref.getObjectId());
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-checkout">git checkout</a>
     *
     * @return SHA-1 of HEAD after checkout
     */
    public String checkOutBranch(String branchName) throws GitAPIException {
        Ref ref = _git.checkout().setName(branchName).call();
        return ObjectId.toString(ref.getObjectId());
    }

    /**
     * Checks out branch ({@link #checkOutBranch(String)}), and
     * encapsulates <a href="https://git-scm.com/docs/git-branch">git branch -D</a>
     *
     * @return name of deleted branch as ref ("refs/heads/BRANCH")
     */
    public String checkoutMasterAndDeleteBranch(String branchName) throws GitAPIException {
        checkOutBranch("master");
        return getOnlyElement(_git.branchDelete().setForce(true).setBranchNames(branchName).call());
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-merge">git-merge --no-ff</a>
     *
     * @return SHA-1 of HEAD after merge (merge commit)
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

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-reset">git reset --hard</a>
     *
     * @return SHA-1 of HEAD after reset
     */
    public String resetHard() throws GitAPIException {
        Ref ref = _git.reset().setMode(ResetCommand.ResetType.HARD).call();
        return ObjectId.toString(ref.getObjectId());
    }

    /**
     * Encapsulates <a href="https://git-scm.com/docs/git-reset">git reset --hard</a>
     *
     * @return SHA-1 of HEAD after reset
     */
    public String resetHardTo(String sha1OrBranch) throws GitAPIException {
        Ref ref = _git.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef(sha1OrBranch)
                .call();
        return ObjectId.toString(ref.getObjectId());
    }

    public String getHeadSha1() throws IOException {
        return ObjectId.toString(_git.getRepository().resolve("HEAD"));
    }

    public String getCurrentBranchName() throws IOException {
        return _git.getRepository().getBranch();
    }

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
    public Optional<String> getFileContentOfRevision(String revisionString, String filePath) throws IOException, GitAPIException {
        ObjectId revisionObjectId = _git.getRepository().resolve(revisionString);
        try (RevWalk revWalk = new RevWalk(_git.getRepository());
             TreeWalk treeWalk = new TreeWalk(_git.getRepository())) {
            RevCommit parsedCommit = revWalk.parseCommit(revisionObjectId);
            RevTree tree = parsedCommit.getTree();
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            if (!treeWalk.next()) {
                return Optional.empty();
            }
            // get first matching only
            ObjectId fileObjectId = treeWalk.getObjectId(0);
            ObjectLoader loader = _git.getRepository().open(fileObjectId, Constants.OBJ_BLOB);
            return Optional.of(new String(loader.getBytes()));
        }
    }

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
    public List<String> lsTree(String revisionString, String directoryPath) throws IOException, GitAPIException {
        ObjectId objectId = _git.getRepository().resolve(revisionString);
        List<String> result = new ArrayList<>();
        TreeFilter filter;
        // HACK - because globs are not supported and passing an empty String or "/" will result in an IllegalArgumentException
        if (directoryPath.isEmpty() || directoryPath.equals(".") || directoryPath.equals("*") || directoryPath.equals("/")) {
            filter = TreeFilter.ALL;
        } else {
            filter = PathFilter.create(directoryPath);
        }
        try (RevWalk revWalk = new RevWalk((_git.getRepository()));
             TreeWalk treeWalkRecursive = new TreeWalk(_git.getRepository())
        ) {
            RevCommit parsedCommit = revWalk.parseCommit(objectId);
            RevTree tree = parsedCommit.getTree();
            treeWalkRecursive.addTree(tree);
            treeWalkRecursive.setRecursive(true);
            treeWalkRecursive.setFilter(filter);
            while (treeWalkRecursive.next()) {
                result.add(new String(treeWalkRecursive.getRawPath()));
            }
        }
        return result;
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
