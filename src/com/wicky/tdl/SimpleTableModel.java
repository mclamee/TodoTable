package com.wicky.tdl;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;

/**
 * Table Model Class for Simple Table: 
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class SimpleTableModel extends DefaultTableModel {
    
    private static final long serialVersionUID = -3231068754665325732L;
    
    private static final Logger LOG = Logger.getLogger(SimpleTableModel.class);
    
    private Vector<String> titles;
    private List<Class<?>> columnTypes;
    private Vector<Vector<Object>> data;
    
    private transient Stack<Vector<Object>> undoList = new Stack<Vector<Object>>(); 
    
    public SimpleTableModel() {
        
        titles = new Vector<String>(Arrays.asList(new String[]{"Id","Desc","Detail", "Done"}));
        columnTypes = Arrays.asList(new Class<?>[]{String.class, String.class, String.class, Boolean.class});
        
        data = new Vector<Vector<Object>>();

        data.add(new Vector<Object>(Arrays.asList(new Object[]{null,"The 1st Task I need to do","10%", new Boolean(false)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{null,"The Task In Progress","1. Plan(50%) 2. Get it Done", new Boolean(false)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{null,"The Task Already Done","100%", new Boolean(true)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{null,"Call William in the afternoon","18702832137", new Boolean(false)})));
        data.add(new Vector<Object>(Arrays.asList(new Object[]{null,"Write feedback Email to William","quiet_dog@163.com", new Boolean(false)})));
        
        this.setDataVector(data, titles);
    }
    
    @Override
    public Object getValueAt(int row, int column) {
        if (column == 0) {
            return row + 1;
        }
        return super.getValueAt(row, column);
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
        this.addRow(new Object[]{null, "", "", false});
        LOG.debug("Added New Row at row " + this.data.size());
        Vector<Object> item = this.data.get(this.data.size() - 1);
        System.out.println("push item: " + item);
        undoList.push(item);
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
        this.data = data;
        this.setDataVector(data, titles);
    }

    @Override
    public void removeRow(int row) {
        Vector<Object> item = this.data.get(row);
        LOG.debug("Deleted Row at row " + row +", with data: " + item);
        undoList.push(item);
        super.removeRow(row);
    }

}