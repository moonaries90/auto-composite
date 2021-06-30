package com.huafon.common.auto.composite.exception;

public class CompositeException extends RuntimeException {

    public CompositeException(String msg) {
        super(msg);
    }

    public CompositeException(Throwable t) {
        super(t);
    }

    public CompositeException(String msg, Throwable t) {
        super(msg, t);
    }
}
