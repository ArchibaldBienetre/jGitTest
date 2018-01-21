package com.example.jgit;

import java.io.File;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;



public class GitWrapper {

    private final Git _git;

    /**
     * Create or open a GIT repository at the given directory
     */
    public GitWrapper(File directory) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
                builder.setGitDir(directory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(); // scan up the file system tree
        File hiddenGitDir = new File(builder.getGitDir(), ".git");
        if (!hiddenGitDir.exists()) {
            Git.init().setDirectory(directory).call();
        }
        Repository repository = builder.build();
        _git = new Git(repository);
    }
}