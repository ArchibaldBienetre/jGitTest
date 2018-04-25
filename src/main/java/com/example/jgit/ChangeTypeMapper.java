package com.example.jgit;

import org.eclipse.jgit.diff.DiffEntry;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ChangeTypeMapper {

    ChangeTypeMapper INSTANCE = Mappers.getMapper(ChangeTypeMapper.class);

    GitDiffType convert(DiffEntry.ChangeType changeType);
}
