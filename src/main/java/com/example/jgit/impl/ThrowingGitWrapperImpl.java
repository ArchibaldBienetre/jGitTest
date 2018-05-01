package com.example.jgit.impl;

import com.example.jgit.GitDiffType;
import com.example.jgit.ThrowingGitWrapper;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.revwalk.filter.SkipRevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static com.example.jgit.impl.ChangeTypeMapper.INSTANCE;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.stream.Collectors.toMap;

/**
 * Encapsulates a GIT repository in the file system using <a href="http://wiki.eclipse.org/JGit/User_Guide">jGit</a>
 */
public class ThrowingGitWrapperImpl implements ThrowingGitWrapper {

    /**
     * Create or open a GIT repository at the given directory
     */
    public static ThrowingGitWrapper createForLocalOnlyRepository(File directory) throws IOException, GitAPIException {
        return new ThrowingGitWrapperImpl(directory);
    }

    private final Git _git;

    @VisibleForTesting
    ThrowingGitWrapperImpl(File directory) throws IOException, GitAPIException {
        _git = localSetup(directory);
    }

    private Git localSetup(File directory) throws IOException, GitAPIException {
        // using thread-safe FileRepository
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setWorkTree(directory);
        File hiddenGitDir = new File(directory, ".git");
        if (!hiddenGitDir.exists()) {
            // Calling git init on an existing repository should not do much harm  -
            // BUT but own hook scripts that are not part of the init template may be replaced.
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

    @Override
    public void add(String filePattern) throws GitAPIException {
        _git.add().addFilepattern(filePattern).call();
        // workaround: AddCommand does not consider removed files in the current version, only if updated is set true
        // (but then added files are ignored, so we do 2 calls here)
        _git.add().setUpdate(true).addFilepattern(filePattern).call();
    }

    @Override
    public void addAll() throws GitAPIException {
        add(".");
    }

    @Override
    public Set<String> clean() throws GitAPIException {
        return _git.clean()
                .setCleanDirectories(true)
                .setForce(true)
                .setIgnore(false)
                .call();
    }

    @Override
    public String commit(String message) throws GitAPIException {
        RevCommit revision = _git.commit().setMessage(message).call();
        return ObjectId.toString(revision);
    }

    @Override
    public String getLastLogEntry() throws GitAPIException {
        RevCommit logEntry = getOnlyElement(_git.log().setMaxCount(1).call());
        return logEntry.getId().toString() + ": " + logEntry.getShortMessage();
    }

    @Override
    public String getLastLogSha1() throws GitAPIException {
        RevCommit logEntry = getOnlyElement(_git.log().setMaxCount(1).call());
        return ObjectId.toString(logEntry);
    }

    @Override
    public String getLastLogMessage() throws GitAPIException {
        RevCommit logEntry = getOnlyElement(_git.log().setMaxCount(1).call());
        return logEntry.getShortMessage();
    }

    @Override
    public String createBranchAndCheckout(String branchName) throws GitAPIException {
        Ref ref = _git.checkout().setCreateBranch(true).setName(branchName).call();
        return ObjectId.toString(ref.getObjectId());
    }

    @Override
    public String checkOutBranch(String branchName) throws GitAPIException {
        Ref ref = _git.checkout().setName(branchName).call();
        return ObjectId.toString(ref.getObjectId());
    }

    @Override
    public String checkoutMasterAndDeleteBranch(String branchName) throws GitAPIException {
        checkOutBranch("master");
        return getOnlyElement(_git.branchDelete().setForce(true).setBranchNames(branchName).call());
    }

    @Override
    public String merge(String branchName) throws GitAPIException {
        Optional<Ref> branchWithMatchingName = findBranchByName(branchName);
        Ref aCommit = branchWithMatchingName.orElseThrow(() -> new IllegalArgumentException("Branch does not exist: " + branchName));
        MergeResult mergeResult = _git.merge()
                .include(aCommit)
                .setCommit(true) // no dry run
                .setFastForward(MergeCommand.FastForwardMode.NO_FF) // create a merge commit
                .call();
        return ObjectId.toString(mergeResult.getNewHead());
    }

    @Override
    public String resetHard() throws GitAPIException {
        Ref ref = _git.reset().setMode(ResetCommand.ResetType.HARD).call();
        return ObjectId.toString(ref.getObjectId());
    }

    @Override
    public String resetHardTo(String sha1OrBranch) throws GitAPIException {
        Ref ref = _git.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef(sha1OrBranch)
                .call();
        return ObjectId.toString(ref.getObjectId());
    }

    @Override
    public String getHeadSha1() throws IOException {
        return ObjectId.toString(_git.getRepository().resolve("HEAD"));
    }

    @Override
    public String getCurrentBranchName() throws IOException {
        return _git.getRepository().getBranch();
    }

    @Override
    public Optional<String> getFileContentOfRevision(String revisionString, String filePath) throws IOException {
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

    @Override
    public List<String> lsTree(String revisionString, String directoryPath) throws IOException {
        ObjectId objectId = _git.getRepository().resolve(revisionString);
        List<String> result = new ArrayList<>();
        TreeFilter filter;
        // HACK - because globs are not supported and passing an empty String or "/" will result in an IllegalArgumentException
        if (directoryPath.isEmpty() || directoryPath.equals(".") || directoryPath.equals("*") || directoryPath.equals("/")) {
            filter = TreeFilter.ALL;
        } else {
            filter = PathFilter.create(directoryPath);
        }
        try (RevWalk revWalk = new RevWalk(_git.getRepository());
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

    @Override
    public List<String> getCommitsBetween(String olderExclusive, String youngerExclusive) throws IOException {
        List<String> result = new ArrayList<>();
        try (RevWalk revWalk = new RevWalk(_git.getRepository())) {
            revWalk.sort(RevSort.TOPO);
            RevFilter revFilter = SkipRevFilter.create(1);
            revWalk.setRevFilter(revFilter);
            ObjectId oldRevisionId = _git.getRepository().resolve(olderExclusive);
            RevCommit oldRevisionCommit = revWalk.parseCommit(oldRevisionId);
            ObjectId youngRevisionId = _git.getRepository().resolve(youngerExclusive);
            RevCommit youngRevisionCommit = revWalk.parseCommit(youngRevisionId);

            if (oldRevisionCommit.equals(youngRevisionCommit)) {
                return result;
            }
            revWalk.markStart(youngRevisionCommit);
            for (RevCommit revision : revWalk) {
                if (revision.equals(oldRevisionCommit)) {
                    break;
                }
                result.add(ObjectId.toString(revision));
            }
        }
        return result;
    }

    @Override
    public Optional<String> getMergeBase(String revisionString1, String revisionString2) throws IOException {
        try (RevWalk revWalk = new RevWalk(_git.getRepository())) {
            revWalk.sort(RevSort.TOPO);
            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            ObjectId revisionId1 = _git.getRepository().resolve(revisionString1);
            RevCommit parsedCommit = revWalk.parseCommit(revisionId1);
            revWalk.markStart(parsedCommit);
            ObjectId revisionId2 = _git.getRepository().resolve(revisionString2);
            RevCommit parsedCommit2 = revWalk.parseCommit(revisionId2);
            revWalk.markStart(parsedCommit2);
            Iterator<RevCommit> it = revWalk.iterator();
            if (it.hasNext()) {
                RevCommit revision = it.next();
                return Optional.of(ObjectId.toString(revision));
            }
        }
        // in case of abandoned commits etc
        return Optional.empty();
    }

    @Override
    public Map<String, GitDiffType> getFileToDiffTypeForRevision(String revisionStringOld, String revisionStringNew) throws IOException {
        return getFileToDiffTypeForRevision(revisionStringOld, revisionStringNew, false);
    }

    @Override
    public Map<String, GitDiffType> getFileToDiffTypeForRevision(String revisionStringOld, String revisionStringNew, boolean recognizeRenames) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(_git.getRepository());
            formatter.setDetectRenames(recognizeRenames);
            ObjectId revisionIdOld = _git.getRepository().resolve(revisionStringOld);
            ObjectId revisionIdNew = _git.getRepository().resolve(revisionStringNew);
            List<DiffEntry> diffs = formatter.scan(revisionIdOld, revisionIdNew);
            return diffs.stream().collect(toMap(
                    diffEntry -> diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE ?
                            diffEntry.getOldPath() :
                            diffEntry.getNewPath(),
                    diffEntry -> INSTANCE.convert(diffEntry.getChangeType())));
        }
    }

    @Override
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
