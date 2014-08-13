package com.wicky.tdl;

/**
 * Indicates that the method is not supported:
 * 
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class NotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 7601825871182715187L;

    public NotSupportedException() {
        super("This method is not supported.");
    }
}
