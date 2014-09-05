package com.wicky.tdl;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

/**
 * Table Model Class for Simple Table: 
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class SimpleTableModel extends DefaultTableModel {
    
    private static final long serialVersionUID = -3231068754665325732L;
    
    private Vector<String> titles;
    private List<Class<?>> columnTypes;
    private Vector<Vector<Object>> data;
    
    private int id;
    
    public SimpleTableModel() {
        
        titles = new Vector<String>(Arrays.asList(new String[]{"Id","Desc","Detail", "Done"}));
        columnTypes = Arrays.asList(new Class<?>[]{String.class, String.class, String.class, Boolean.class});
        
        data = new Vector<Vector<Object>>();
        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"The 1st Task I need to do","10%", new Boolean(false)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"The Task In Progress","1. Plan(50%) 2. Get it Done", new Boolean(false)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"The Task Already Done","100%", new Boolean(true)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"Call William in the afternoon","18702832137", new Boolean(false)})));
        
        this.setDataVector(data, titles);
    }
    
    public Class<?> getColumnClass(int col) {
        return columnTypes.get(col);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if(columnIndex == 0)return false;
        return true;
    }
    
    @Override
    public void setValueAt(Object aValue, int row, int column) {
        super.setValueAt(aValue, row, column);
    }
    
    public void addRow(){
        this.addRow(new Object[]{++id, "", "", false});
    }
    
    public int getColumnIdx(String colName) {
        if(colName != null && colName.length() > 0)
        for(int i=0; i<titles.size(); i++){
            if(colName.equals(titles.get(i))){
                return i;
            }
        }
        return -1;
    }

    public Vector<?> exportData() {
        return data;
    }

    public void initData(Vector<Vector<Object>> data) {
        resetIdField(data);
        this.data = data;
        this.setDataVector(data, titles);
    }

    private void resetIdField(Vector<?> data) {
        int i = 0;
        for (@SuppressWarnings("unchecked")
        Iterator<Vector<Object>> iterator = (Iterator<Vector<Object>>) data.iterator();iterator.hasNext();) {
            Vector<Object> entry = iterator.next();
            entry.set(0, ++i);
        }
        this.id = i;
    }

    
    @Override
    public void removeRow(int row) {
        super.removeRow(row);
        resetIdField(this.data);
    }
}