package com.wicky.tdl;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.FrameBorderStyle;

public class SimpleTable extends JTable implements ListSelectionListener {


    private static final long serialVersionUID = -3747203421484558542L;
    
    private static final String APP_NAME = "Simple ToDo Table";
    private static final Dimension APP_SIZE = new Dimension(500, 300);
    
    private static final String RES_LOGO = "/logo.png";
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String PROFILE_HOME = "/.todoProfile";
    private static final String DATA_FILE = "/tableModel.db";
    private static final String LOCK_FILE = "/runtime.lck";
    private static final int SAVE_TIMEOUT = 1000*60*5;
    

    private File dataFile;
    
    private SimpleTableModel dataModel;
    private JFrame frame;
    private JPanel mainPan;
    private JButton btnAdd;

    private SystemTray sysTray;// 当前操作系统的托盘对象
    private TrayIcon trayIcon;// 当前对象的托盘
    private ImageIcon icon;
    
    private ObjectInputStream in;
    private ObjectOutputStream out;

    protected boolean dataChanged;

    private Timer timer;

    protected boolean runing;

    private static File profile;

    public SimpleTable() throws IOException {
        dataFile = new File(profile, DATA_FILE);
        if(dataFile.isDirectory())dataFile.delete();
        if(!dataFile.exists()){
            dataFile.createNewFile();
            System.out.println("No data file exists, creating new file: ["+dataFile.getAbsolutePath()+"]");
        }
        Vector<?> data = null;
        try {
            System.out.print("Reading data file ...");
            in = new ObjectInputStream(new FileInputStream(dataFile));
            data = (Vector<?>) in.readObject();
            System.out.println(" Success!");
        } catch (EOFException e) {
            System.out.println(" Canceled: file is empty.");
        } catch (ClassNotFoundException e2) {
            System.out.println(" Canceled: file format issue.");
        }finally{
            if(in != null){
                in.close();
            }
        }
        dataModel = new SimpleTableModel();
        dataModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                dataChanged = true;
            }
        });
        if(data != null){
            dataModel.initData(data);
        }
        this.setModel(dataModel);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int r = SimpleTable.this.rowAtPoint(e.getPoint());
                if (r >= 0 && r < SimpleTable.this.getRowCount()) {
                    SimpleTable.this.setRowSelectionInterval(r, r);
                } else {
                    SimpleTable.this.clearSelection();
                }

                int rowindex = SimpleTable.this.getSelectedRow();
                if (rowindex < 0)
                    return;
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem delItm = new JMenuItem("Delete");
                    delItm.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dataModel.removeRow(SimpleTable.this.getSelectedRow());
                            refreshTable();
                        }
                    });
                    popup.add(delItm);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        ListSelectionModel listMod = this.getSelectionModel();
        listMod.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listMod.addListSelectionListener(this);

        btnAdd = new JButton("Add");
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataModel.addRow();
                refreshTable();
                TableCellEditor ce = SimpleTable.this.getCellEditor();
                if(ce != null){
                    ce.cancelCellEditing();
                }
            }
        });

        mainPan = new JPanel();
        mainPan.setLayout(new BorderLayout());
        mainPan.add(btnAdd, BorderLayout.NORTH);

        JScrollPane scrollpane = new JScrollPane(this);
        scrollpane.setPreferredSize(APP_SIZE);
        mainPan.add(scrollpane, BorderLayout.CENTER);

        frame = new JFrame(APP_NAME);
        icon = new ImageIcon(this.getClass().getResource(RES_LOGO));// 托盘图标
        frame.setIconImage(icon.getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPan);
        frame.pack();
        frame.setVisible(true);

        createTrayIcon();
        // 添加窗口事件,将托盘添加到操作系统的托盘
        frame.addWindowListener(new WindowAdapter() {
            public void windowIconified(WindowEvent e) {
                try {
                    sysTray.add(trayIcon);// 将托盘添加到操作系统的托盘
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            frame.setVisible(false);// 使得当前的窗口隐藏
                            frame.setState(JFrame.NORMAL);
                        }
                    });
                } catch (AWTException e1) {
                    e1.printStackTrace();
                }
            }
        });
        adjustColumnWidth();
        
        frame.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowClosing(WindowEvent e) {
                TableCellEditor ce = SimpleTable.this.getCellEditor();
                if(ce != null){
                    ce.stopCellEditing();
                }
                timer.cancel();
                if(!runing){
                    System.out.println("Window Closing!");
                    saveDataToFile();
                }
                super.windowClosing(e);
            }
        });
        
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            
            @Override
            public void run() {
                if(dataChanged){
                    runing = true;
                    System.out.println("Timmer Start!");
                    saveDataToFile();
                    runing = false;
                }
                dataChanged = false;
            }

        }, new Date(System.currentTimeMillis() + SAVE_TIMEOUT), SAVE_TIMEOUT);
        
        this.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_DELETE){
                    int selectedRow = SimpleTable.this.getSelectedRow();
                    if(selectedRow != -1){
                        TableCellEditor ce = SimpleTable.this.getCellEditor();
                        if(ce != null){
                            ce.cancelCellEditing();
                        }
                        int rowCount = dataModel.getRowCount();
                        if(rowCount != 0){
                            int result = JOptionPane.showConfirmDialog(SimpleTable.this, "Are you sure to delete this row?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                            if(result == JOptionPane.YES_OPTION){
                                dataModel.removeRow(selectedRow);
                                if(selectedRow < dataModel.getRowCount()){
                                    SimpleTable.this.setRowSelectionInterval(selectedRow, selectedRow);
                                }
                                TableCellEditor ce2 = SimpleTable.this.getCellEditor();
                                if(ce2 != null){
                                    ce2.cancelCellEditing();
                                }
                                refreshTable();
                            }
                        }
                    }

                }
            }
        });
    }

    private void saveDataToFile() {
        try {
            System.out.print("Saving data ...");
            out = new ObjectOutputStream(new FileOutputStream(dataFile));
            out.writeObject(dataModel.exportData());
            System.out.println(" Saved.");
        } catch (FileNotFoundException e1) {
            System.out.println(" Canceled: File cannot open.");
        } catch (IOException e1) {
            System.out.println(" Error: " + e1);
        }finally{
            try {
                out.flush();
                out.close();
            } catch (IOException e1) {
                System.out.println("IO Exception: " + e1);
            }
        }
    }
    
    /**
     * 创建系统托盘的对象 步骤: 1,获得当前操作系统的托盘对象 2,创建弹出菜单popupMenu 3,创建托盘图标icon 4,创建系统的托盘对象trayIcon
     */
    public void createTrayIcon() {
        sysTray = SystemTray.getSystemTray();// 获得当前操作系统的托盘对象
        PopupMenu popupMenu = new PopupMenu();// 弹出菜单
        MenuItem mi = new MenuItem("Show");
        MenuItem exit = new MenuItem("Exit");
        popupMenu.add(mi);
        popupMenu.add(exit);
        // 为弹出菜单项添加事件
        ActionListener backToFrontListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sysTray.remove(trayIcon);
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        frame.setVisible(true);
                        frame.toFront();
                        frame.requestFocus();
                    }
                });
            }
        };
        mi.addActionListener(backToFrontListener);
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        trayIcon = new TrayIcon(icon.getImage(), APP_NAME, popupMenu);
        trayIcon.addActionListener(backToFrontListener);
    }

    
    /**
    * 调整列宽
    */
    private void adjustColumnWidth() {
    // Tweak the appearance of the table by manipulating its column model
    TableColumnModel colmodel = this.getColumnModel();
    this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    // Set column widths
    colmodel.getColumn(0).setPreferredWidth(10);
    colmodel.getColumn(1).setPreferredWidth(200);
    colmodel.getColumn(2).setPreferredWidth(100);
    colmodel.getColumn(3).setPreferredWidth(10);
    }
    
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component com = super.prepareRenderer(renderer, row, column);
        
        if(getValueAt(row, 3).equals(true)){
            com.setForeground(Color.GRAY);
        }else{
            com.setForeground(Color.BLACK);
        }
        return com;
    }
    
    private void refreshTable() {
        this.setModel(this.getModel());
        this.revalidate();
    }
    
    private static boolean lockInstance(final File file) {
        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        } catch (Exception e) {
                            System.err.println("Unable to remove lock file: " + file);
                            System.err.println(e);
                        }
                    }
                });
                System.out.println("OK!");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Unable to create and/or lock file: " + file);
            System.err.println(e);
        }
        System.out.println("Failed!");
        return false;
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        profile = new File(USER_HOME + PROFILE_HOME);
        if(profile.isFile())profile.delete();
        profile.mkdirs();
        File lockFile = new File(profile, LOCK_FILE);
        System.out.print("Checking Lock File ["+lockFile+"] ... ");
        if(lockInstance(lockFile)){
            try {
                BeautyEyeLNFHelper.frameBorderStyle = FrameBorderStyle.translucencyAppleLike;
                BeautyEyeLNFHelper.translucencyAtFrameInactive = false;
                UIManager.put("RootPane.setupButtonVisible", false);
                BeautyEyeLNFHelper.launchBeautyEyeLNF();
            } catch (Exception r) {
            }

            Font font = new Font("微软雅黑", Font.PLAIN, 12);
            UIManager.put("Frame.titleFont", font);
            UIManager.put("Menu.font", font);
            UIManager.put("MenuItem.font", font);
            UIManager.put("TitledBorder.font", font);
            UIManager.put("InternalFrame.font", font);
            UIManager.put("InternalFrame.titleFont", font);
            UIManager.put("Table.font", font);
            UIManager.put("TableHeader.font", font);
            UIManager.put("Button.font", font);
            UIManager.put("Label.font", font);
            UIManager.put("List.font", font);
            
            try {
                new SimpleTable();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("Already Running another instance, exiting program ...");
        }
    }
}
