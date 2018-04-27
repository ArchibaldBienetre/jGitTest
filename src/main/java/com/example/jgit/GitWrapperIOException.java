package com.example.jgit;

import com.example.jgit.impl.ThrowingGitWrapperImpl;

import java.io.IOException;

/**
 * Generic Exceptionn thnown by {@link ThrowingGitWrapperImpl} to hide the underlying implementation.
 * While very similar to {@link GitWrapperException}, it allows users to catch any I/O-related Exceptions separately.
 * Those are less likely to be recoverable that the ones wrapped by {@link GitWrapperException}.
 */
public class GitWrapperIOException extends RuntimeException {
    public GitWrapperIOException(IOException e) {
        super(e);
    }
}
