package com.examly.springapp.exception;
public class UnauthorisedAccessException extends RuntimeException {
    public UnauthorisedAccessException(String m) {
        super(m);
    }
}
