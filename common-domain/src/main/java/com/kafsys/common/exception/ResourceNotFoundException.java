package com.kafsys.common.exception;

public class ResourceNotFoundException extends KafsysException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier, "RESOURCE_NOT_FOUND");
    }
}
