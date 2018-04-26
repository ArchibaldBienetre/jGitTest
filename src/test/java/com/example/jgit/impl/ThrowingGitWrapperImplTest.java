package com.example.jgit.impl;

import com.example.jgit.ThrowingGitWrapper;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ThrowingGitWrapperImplTest extends AbstractGitWrapperImplTest<ThrowingGitWrapper> {
    @Override
    protected ThrowingGitWrapper createGitWrapper() throws IOException, GitAPIException {
        ThrowingGitWrapper wrapper = ThrowingGitWrapperImpl.createForLocalOnlyRepository(_tempDir);
        return (ThrowingGitWrapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ThrowingGitWrapper.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                try {
                    return method.invoke(wrapper, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });
    }
}
