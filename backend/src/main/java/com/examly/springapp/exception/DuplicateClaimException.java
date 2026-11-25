package com.examly.springapp.exception;
public class DuplicateClaimException extends RuntimeException {
    public DuplicateClaimException(String m) {
        super(m);
    }
}
