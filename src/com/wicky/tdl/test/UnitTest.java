/**
 * @author williamz 2014年8月13日
 *
 * Copyright (c) 2013, Synnex and/or its affiliates. All rights reserved.
 * SYNNEX PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.wicky.tdl.test;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import com.wicky.tdl.rmi.BringToFrontImpl;


/**
 * Test Class
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class UnitTest {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(34971);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34972);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34973);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34974);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34975);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34976);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34977);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34978);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            LocateRegistry.createRegistry(34979);
            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
//        try {
//            LocateRegistry.createRegistry(34980);
//            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
//        } catch (RemoteException | MalformedURLException e) {
//            e.printStackTrace();
//        }
//        try {
//            LocateRegistry.createRegistry(34981);
//            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
//        } catch (RemoteException | MalformedURLException e) {
//            e.printStackTrace();
//        }
//        try {
//            LocateRegistry.createRegistry(34982);
//            Naming.rebind("//127.0.0.1:34971/IBringToFront" , new BringToFrontImpl(null));
//        } catch (RemoteException | MalformedURLException e) {
//            e.printStackTrace();
//        }
    }
}

