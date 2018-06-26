package com.example.jgit;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Non-throwing version of the {@link ThrowingGitWrapper} interface.
 * Use {@link GitWrapperFactory#createForLocalOnlyRepository(File)} to create
 */
public interface GitWrapper extends ThrowingGitWrapper {
    @Override
    void add(String filePattern);

    @Override
    void addAll();

    @Override
    Set<String> clean();

    @Override
    String commit(String message);

    @Override
    String getLastLogEntry();

    @Override
    Instant getLastCommitTimeRoundedToSeconds();

    @Override
    String getLastLogSha1();

    @Override
    String getLastLogMessage();

    @Override
    String createBranchAndCheckout(String branchName);

    @Override
    String checkOutBranch(String branchName);

    @Override
    String checkoutMasterAndDeleteBranch(String branchName);

    @Override
    String merge(String branchName);

    @Override
    String resetHard();

    @Override
    String resetHardTo(String sha1OrBranch);

    @Override
    String getHeadSha1();

    @Override
    String getCurrentBranchName();

    @Override
    Optional<String> getFileContentOfRevision(String revisionString, String filePath);

    @Override
    List<String> lsTree(String revisionString, String directoryPath);

    @Override
    List<String> getCommitsBetween(String olderExclusive, String youngerExclusive);

    @Override
    Optional<String> getMergeBase(String revisionString1, String revisionString2);

    @Override
    Map<String, GitDiffType> getFileToDiffTypeForRevision(String revisionStringOld, String revisionStringNew);

    @Override
    Map<String, GitDiffType> getFileToDiffTypeForRevision(String revisionStringOld, String revisionStringNew, boolean recognizeRenames);

    @Override
    boolean doesBranchExist(String branchName);
}
