package com.wicky.tdl.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Vector;

public class SubVector extends Vector<Object> implements Serializable{
    private static final long serialVersionUID = -8903166408920564591L;
    
    public SubVector(String desc, String detail, Boolean flag) {
        super(Arrays.asList(new Object[]{null, desc, detail, flag}));
    }
    
    public SubVector(String desc, String detail) {
        super(Arrays.asList(new Object[]{null, desc, detail, Boolean.FALSE}));
    }
    
    @Override
    public synchronized String toString() {
        return "{desc:\""+this.get(1)+"\", detail:\""+this.get(2)+"\", done:\""+this.get(3)+"\"}";
    }
}