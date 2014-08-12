package com.wicky.tdl;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

/**
 * Table Model Class for Simple Table: 
 * @author williamz@synnex.com 2014年7月2日 下午12:32:34
 */
public class SimpleTableModel extends DefaultTableModel {
    
    private static final long serialVersionUID = -3231068754665325732L;
    
    private Vector<String> titles;
    private List<Class<?>> columnTypes;
    private Vector<?> data;
    
    private int id;
    
    public SimpleTableModel() {
        
        titles = new Vector<String>(Arrays.asList(new String[]{"Id","Desc","Detail", "Done"}));
        columnTypes = Arrays.asList(new Class<?>[]{String.class, String.class, String.class, Boolean.class});
        
        data = new Vector<Vector<Object>>();
//        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"Andrews","Details of Andrews", new Boolean(false)})));
//        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"Tom","Details of Tom", new Boolean(false)})));
//        data.add(new Vector<Object>(Arrays.asList(new Object[]{++id,"Sida","Details of Sida", new Boolean(false)})));
        
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

    public void initData(Vector<?> data) {
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