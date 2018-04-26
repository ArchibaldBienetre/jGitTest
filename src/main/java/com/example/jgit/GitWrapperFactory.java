package com.example.jgit;

import com.example.jgit.impl.ThrowingGitWrapperImpl;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GitWrapperFactory {

    public static GitWrapper createForLocalOnlyRepository(File directory) {
        ThrowingGitWrapper wrapper;
        try {
            wrapper = ThrowingGitWrapperImpl.createForLocalOnlyRepository(directory);
        } catch (IOException e) {
            throw new GitWrapperException(e);
        } catch (GitAPIException e) {
            throw new GitWrapperException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (GitWrapper) Proxy.newProxyInstance(GitWrapperFactory.class.getClassLoader(), new Class<?>[]{GitWrapper.class}, (proxy, method, args) -> {
            try {
                try {
                    Method matchingThrowingMethod = ThrowingGitWrapper.class.getMethod(method.getName(), method.getParameterTypes());
                    return matchingThrowingMethod.invoke(wrapper, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            } catch (IOException e) {
                throw new GitWrapperException(e);
            } catch (GitAPIException e) {
                throw new GitWrapperException(e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }
}
