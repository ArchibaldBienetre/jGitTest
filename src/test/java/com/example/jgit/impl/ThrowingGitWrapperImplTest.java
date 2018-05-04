package com.example.jgit.impl;

import com.example.jgit.ThrowingGitWrapper;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

public class ThrowingGitWrapperImplTest extends AbstractGitWrapperImplTest<ThrowingGitWrapper> {
    @Override
    protected ThrowingGitWrapper createGitWrapper() throws IOException, GitAPIException {
        return ThrowingGitWrapperImpl.createForLocalOnlyRepository(_tempDir);
    }
}
