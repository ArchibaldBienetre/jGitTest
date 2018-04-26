package com.example.jgit.impl;

import com.example.jgit.GitDiffType;
import org.eclipse.jgit.diff.DiffEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ChangeTypeMapperTest {

    @Test
    public void test_convert() {
        for (DiffEntry.ChangeType originalChangeType : DiffEntry.ChangeType.values()) {
            GitDiffType actual = ChangeTypeMapper.INSTANCE.convert(originalChangeType);

            assertNotNull(actual);
            assertEquals(originalChangeType.toString(), actual.toString());
        }
    }
}