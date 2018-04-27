package com.example.jgit;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitWrapperArchitectureTest {


    @Test
    public void test_that_GitWrapper_interface_overrides_all_methods_of_ThrowingGitWrapper() throws Exception {
        for (Method throwingMethod : ThrowingGitWrapper.class.getMethods()) {
            Method actual = GitWrapper.class.getMethod(throwingMethod.getName(), throwingMethod.getParameterTypes());
            assertTrue(actual.getAnnotatedExceptionTypes().length == 0);
        }
    }
}
