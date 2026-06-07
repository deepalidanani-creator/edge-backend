package com.emeritus.edge_backend.exception;

public class MissingTenantHeaderException extends RuntimeException {

    public MissingTenantHeaderException() {
        super("X-Tenant-Id header is required.");
    }
}
