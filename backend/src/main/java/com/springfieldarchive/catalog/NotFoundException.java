package com.springfieldarchive.catalog;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource) {
        super(resource);
    }
}
