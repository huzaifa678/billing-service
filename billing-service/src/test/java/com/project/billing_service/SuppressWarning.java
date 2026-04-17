package com.project.billing_service;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;

public class SuppressWarning {
    @SuppressWarnings("unchecked")
    static <T> Supplier<T> anySupplier() {
        return any(Supplier.class);
    }
}
