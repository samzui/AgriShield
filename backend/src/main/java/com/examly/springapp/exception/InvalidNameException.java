package com.examly.springapp.exception;
public class InvalidNameException extends RuntimeException {
    public InvalidNameException(String m) {
        super(m);
    }
}
