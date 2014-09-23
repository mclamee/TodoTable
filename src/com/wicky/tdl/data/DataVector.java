package com.wicky.tdl.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class DataVector extends Vector<SubVector> implements Serializable{
    private static final long serialVersionUID = -999861995984107110L;
    
    private Vector<String> titles = new Vector<String>(Arrays.asList(new String[]{"Id","Desc","Detail", "Done"}));
    private List<Class<?>> columnTypes = Arrays.asList(new Class<?>[]{String.class, String.class, String.class, Boolean.class});
    
    public synchronized boolean add(String desc, String detail, Boolean flag) {
        return this.add(new SubVector(desc, detail, flag));
    }
    
    public synchronized boolean add(String desc, String detail) {
        return this.add(new SubVector(desc,detail));
    }
    
    public Vector<String> getTitles() {
        return titles;
    }

    public void setTitles(Vector<String> titles) {
        this.titles = titles;
    }

    public Class<?> getColumnClass(int col) {
        return this.columnTypes.get(col);
    }
    
    public int getColumnIdx(String colName) {
        if(colName != null && colName.length() > 0)
        for(int i=0; i<this.titles.size(); i++){
            if(colName.equals(this.titles.get(i))){
                return i;
            }
        }
        return -1;
    }

    public Object getValueAt(int row, int column) {
        if (column == 0) {
            return row + 1;
        }
        SubVector rowVector = this.elementAt(row);
        return rowVector.elementAt(column);
    }
    
    public synchronized boolean add(Vector<Object> dat) {
        return this.add((String)dat.get(1), (String)dat.get(2), (Boolean)dat.get(3));
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        
        for (int i = 0;i < this.size();i++) {
            SubVector sub = this.get(i);
            sb.append("  ").append(i+1).append(":\"").append(sub).append("\",\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}