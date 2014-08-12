package com.wicky.tdl;
/**
 * Indicates that the method is not supported: 
 * @author williamz@synnex.com 2014年7月2日 下午12:35:39
 */
public class NotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 7601825871182715187L;

    public NotSupportedException() {
        super("This method is not supported.");
    }
}
