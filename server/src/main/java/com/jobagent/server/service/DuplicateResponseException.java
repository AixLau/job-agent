package com.jobagent.server.service;

public class DuplicateResponseException extends RuntimeException {

    private final Object payload;

    public DuplicateResponseException(Object payload) {
        super("duplicate response");
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }
}
