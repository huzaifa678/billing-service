package com.project.billing_service.model.mapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseMapper<E, D> {

    public abstract E convertToEntity(D dto, Object... args);

    public abstract D convertToDto(E entity, Object... args);

    public Collection<E> convertToEntity(Collection<D> dto, Object... args) {
        return dto.stream()
                .map(d -> convertToEntity(d, args))
                .collect(Collectors.toList());
    }

    public Collection<D> convertToDto(Collection<E> entity, Object... args) {
        return entity.stream()
                .map(e -> convertToDto(e, args))
                .collect(Collectors.toList());
    }

    public List<E> convertToEntity(List<D> dto, Object... args) {
        return convertToEntity((Collection<D>) dto, args).stream().toList();
    }

    public List<D> convertToDto(List<E> entity, Object... args) {
        return convertToDto((Collection<E>) entity, args).stream().toList();
    }

    public Set<E> convertToEntity(Set<D> dto, Object... args) {
        return new HashSet<>(convertToEntity((Collection<D>) dto, args));
    }

    public Set<D> convertToDto(Set<E> entity, Object... args) {
        return new HashSet<>(convertToDto((Collection<E>) entity, args));
    }
}
