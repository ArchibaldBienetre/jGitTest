package com.example.jgit;

/**
 * Wrapper around ChangeType in order not to expose jGit internals too much
 */
@SuppressWarnings("unused")
public enum GitDiffType {
    ADD, DELETE, MODIFY, RENAME, COPY;
}
