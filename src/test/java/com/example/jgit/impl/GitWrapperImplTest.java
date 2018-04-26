package com.example.jgit.impl;

import com.example.jgit.GitWrapper;
import com.example.jgit.GitWrapperFactory;
import org.junit.jupiter.api.Test;

public class GitWrapperImplTest extends AbstractGitWrapperImplTest<GitWrapper> {
    @Override
    protected GitWrapper createGitWrapper() {
        return GitWrapperFactory.createForLocalOnlyRepository(_tempDir);
    }

    @Test
    public void test_that_GitWrapper_really_does_not_throw_checked_Exceptions() {
        GitWrapper sut = GitWrapperFactory.createForLocalOnlyRepository(_tempDir);
        sut.resetHard();
        sut.clean();
        sut.getHeadSha1();

        // ... and so on ...
    }


}
