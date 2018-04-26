package com.example.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * Generic Exceptionn thnown by {@link GitWrapperImpl} to hide the underlying implementation.
 */
public class GitWrapperException extends RuntimeException {
    public GitWrapperException(GitAPIException e) {
        super(e);
    }

    public GitWrapperException(IOException e) {
        super(e);
    }
}
