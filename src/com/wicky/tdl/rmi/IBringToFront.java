package com.wicky.tdl.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote Interface for the IBringToFront.
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public interface IBringToFront extends Remote {
    
    public boolean performAction() throws RemoteException;
    
}
