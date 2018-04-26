package com.example.jgit;

import com.example.jgit.impl.ThrowingGitWrapperImpl;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * Generic Exceptionn thnown by {@link ThrowingGitWrapperImpl} to hide the underlying implementation.
 */
public class GitWrapperException extends RuntimeException {
    public GitWrapperException(GitAPIException e) {
        super(e);
    }

    public GitWrapperException(IOException e) {
        super(e);
    }
}
