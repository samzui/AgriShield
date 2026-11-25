package com.examly.springapp.exception;
public class InvalidPhoneException extends RuntimeException {
    public InvalidPhoneException(String m) {
        super(m);
    }
}
