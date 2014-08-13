package com.wicky.tdl.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import com.wicky.tdl.SimpleTodoTable;

/**
 * Remote Class for the IBringToFront.
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class BringToFrontImpl extends UnicastRemoteObject implements IBringToFront {

    private SimpleTodoTable table;

    public BringToFrontImpl(SimpleTodoTable table) throws RemoteException {
        this.setTable(table);
    }

    private static final long serialVersionUID = -6233480963856732731L;

    @Override
    public boolean performAction() throws RemoteException {
        getTable().bringToFront();
        return true;
    }

    public SimpleTodoTable getTable() {
        return table;
    }

    public void setTable(SimpleTodoTable table) {
        this.table = table;
    }
    
}
