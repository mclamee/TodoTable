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
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
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

import org.apache.log4j.Logger;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.FrameBorderStyle;

import sun.misc.BASE64Encoder;

import com.wicky.tdl.rmi.BringToFrontImpl;
import com.wicky.tdl.rmi.IBringToFront;


/**
 * Main class for the simple todo table project
 * @author williamz<quiet_dog@163.com> 2014-08-13
 */
public class SimpleTodoTable extends JTable implements ListSelectionListener {
    private static final long serialVersionUID = -3747203421484558542L;
    private static final Logger LOG;
/////////////////////////////////////////////////////////////////////////////////////////////
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String PROFILE_HOME = "/"+ConfigUtil.get("profileFolder");
    private static final File PROFILE = new File(USER_HOME + PROFILE_HOME);
    static{
        if(PROFILE.isFile()){
            try {
                PROFILE.delete();
            } catch (Exception e) {
                // ignore
            }
        }
        PROFILE.mkdirs();
        
        System.setProperty("PROFILE_HOME", PROFILE.getAbsolutePath());
        System.setProperty("PID", ManagementFactory.getRuntimeMXBean().getName());

        // setup LOG at the end of static
        LOG = Logger.getLogger(SimpleTodoTable.class);
        LOG.warn(".......................... System Start ..........................");
    }

    private static final String APP_NAME = ConfigUtil.get("title");
    private static final String APP_LOGO = System.getProperty("user.dir") + ConfigUtil.get("logo");
    private static final String APP_SYSTRAY_LOGO = System.getProperty("user.dir") + ConfigUtil.get("systray");
    private static final Dimension APP_SIZE = new Dimension(ConfigUtil.getInt("width"), ConfigUtil.getInt("height"));
    private static final int APP_SAVE_INTERVAL = (((int) (1000*60*ConfigUtil.getDouble("saveIntervalMinute"))) < 60000)?60000:((int) (1000*60*ConfigUtil.getDouble("saveIntervalMinute")));
    
    private static final String DATA_FILE = "/"+ConfigUtil.get("dataFile");
    private static final String LOCK_FILE = "/"+ConfigUtil.get("lockFile");
    private static final String HOST = ConfigUtil.get("host");
    private static final int PORT = ConfigUtil.getInt("port");
    
/////////////////////////////////////////////////////////////////////////////////////////////
    
    private File dataFile;
    
    private SimpleTableModel dataModel;
    private JFrame frame;
    private JPanel mainPan;
    private JButton btnAdd;
    private JButton btnClear;

    private SystemTray sysTray;// 当前操作系统的托盘对象
    private TrayIcon trayIcon;// 当前对象的托盘
    private Timer timer;
    
    private ObjectInputStream in;
    private ObjectOutputStream out;

    protected boolean dataChanged;
    protected boolean runing;

    @SuppressWarnings("unchecked")
    public SimpleTodoTable() throws IOException {
        dataFile = new File(PROFILE, DATA_FILE);
        if(dataFile.isDirectory())dataFile.delete();
        if(!dataFile.exists()){
            dataFile.createNewFile();
            LOG.debug("No data file exists, creating new file: ["+dataFile.getAbsolutePath()+"]");
        }
        Vector<Vector<Object>> data = null;
        try {
            LOG.debug("Reading data file ... ");
            in = new ObjectInputStream(new FileInputStream(dataFile));
            data = (Vector<Vector<Object>>) in.readObject();
            LOG.debug(">> Success!");
        } catch (EOFException e) {
            LOG.debug(">> Canceled.");
            LOG.error("File is empty: ", e);
        } catch (ClassNotFoundException e2) {
            LOG.debug(">> Canceled.");
            LOG.error("File format issue: ", e2);
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
                int r = SimpleTodoTable.this.rowAtPoint(e.getPoint());
                if (r >= 0 && r < SimpleTodoTable.this.getRowCount()) {
                    SimpleTodoTable.this.setRowSelectionInterval(r, r);
                } else {
                    SimpleTodoTable.this.clearSelection();
                }

                int rowindex = SimpleTodoTable.this.getSelectedRow();
                if (rowindex < 0)
                    return;
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem delItm = new JMenuItem("Delete");
                    delItm.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dataModel.removeRow(SimpleTodoTable.this.getSelectedRow());
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

        this.setUI(new DragDropRowTableUI());  
        
        Dimension btnSize = new Dimension(150, 25);
        
        btnAdd = new JButton("Add New");
        btnAdd.setPreferredSize(btnSize);
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataModel.addRow();
                refreshTable();
                TableCellEditor ce = SimpleTodoTable.this.getCellEditor();
                if(ce != null){
                    ce.cancelCellEditing();
                }
            }
        });

        btnClear = new JButton("Delete Finishied");
        btnClear.setPreferredSize(btnSize);
        btnClear.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                TableCellEditor ce = SimpleTodoTable.this.getCellEditor();
                if(ce != null){
                    ce.cancelCellEditing();
                }
                int rowCount = dataModel.getRowCount();
                for (int rowId = 0;rowId < rowCount;rowId++) {
                    Boolean value = (Boolean) dataModel.getValueAt(rowId, 3);
                    if(value){
                        dataModel.removeRow(rowId);
                        rowId--;rowCount--;
                    }
                }
                refreshTable();
            }
        });
        
        JPanel northPanel = new JPanel();
        northPanel.add(btnAdd, BorderLayout.CENTER);
        northPanel.add(btnClear, BorderLayout.CENTER);
        
        mainPan = new JPanel();
        mainPan.setLayout(new BorderLayout());
        mainPan.add(northPanel, BorderLayout.NORTH);

        JScrollPane scrollpane = new JScrollPane(this);
        scrollpane.setPreferredSize(APP_SIZE);
        mainPan.add(scrollpane, BorderLayout.CENTER);

        frame = new JFrame(APP_NAME);
        frame.setIconImage(new ImageIcon(APP_LOGO).getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPan);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        createTrayIcon();
        // 添加窗口事件,将托盘添加到操作系统的托盘
        frame.addWindowListener(new WindowAdapter() {
            public void windowIconified(WindowEvent e) {
                minimizeToTray();
            }
        });
        adjustColumnWidth();
        
        frame.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowClosing(WindowEvent e) {
                TableCellEditor ce = SimpleTodoTable.this.getCellEditor();
                if(ce != null){
                    ce.stopCellEditing();
                }
                timer.cancel();
                if(!runing){
                    LOG.debug("Window Closing!");
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
                    LOG.debug("Timmer Start! Save Interval: " + APP_SAVE_INTERVAL);
                    saveDataToFile();
                    runing = false;
                }
                dataChanged = false;
            }

        }, new Date(System.currentTimeMillis() + APP_SAVE_INTERVAL), APP_SAVE_INTERVAL);
        
        this.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_DELETE){
                    int selectedRow = SimpleTodoTable.this.getSelectedRow();
                    if(selectedRow != -1){
                        TableCellEditor ce = SimpleTodoTable.this.getCellEditor();
                        if(ce != null){
                            ce.cancelCellEditing();
                        }
                        int rowCount = dataModel.getRowCount();
                        if(rowCount != 0){
                            int result = JOptionPane.showConfirmDialog(SimpleTodoTable.this, "Are you sure to delete this row?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                            if(result == JOptionPane.YES_OPTION){
                                dataModel.removeRow(selectedRow);
                                if(selectedRow < dataModel.getRowCount()){
                                    SimpleTodoTable.this.setRowSelectionInterval(selectedRow, selectedRow);
                                }
                                TableCellEditor ce2 = SimpleTodoTable.this.getCellEditor();
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
            LOG.debug("Saving data ... ");
            out = new ObjectOutputStream(new FileOutputStream(dataFile));
            out.writeObject(dataModel.exportData());
            LOG.debug(">> Saved.");
        } catch (FileNotFoundException e1) {
            LOG.debug(">> Canceled.");
            LOG.error("File cannot open: ", e1);
        } catch (IOException e1) {
            LOG.debug(">> Canceled.");
            LOG.error("IOException: ", e1);
        }finally{
            try {
                out.flush();
                out.close();
            } catch (IOException e1) {
                LOG.error("IOException: ", e1);
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
                bringToFront();
            }
        };
        mi.addActionListener(backToFrontListener);
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        trayIcon = new TrayIcon(new ImageIcon(APP_SYSTRAY_LOGO).getImage(), APP_NAME, popupMenu);
        trayIcon.addActionListener(backToFrontListener);
    }
    
    public void minimizeToTray() {
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
        LOG.debug("Minimized window to system tray.");
    }
    
    public void bringToFront() {
        sysTray.remove(trayIcon);
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
            }
        });
        LOG.debug("Restored and brought window to front.");
    }

    /**
    * 调整列宽
    */
    private void adjustColumnWidth() {
    // Tweak the appearance of the table by manipulating its column model
    TableColumnModel colmodel = this.getColumnModel();
    this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    // Set column widths
    colmodel.getColumn(0).setPreferredWidth(1);
    colmodel.getColumn(1).setPreferredWidth(250);
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
        LOG.debug("Checking Lock File ["+file+"] ... ");
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
                            LOG.warn(".......................... System Halt ..........................");
                        } catch (Exception e) {
                            LOG.error("Unable to remove lock file: " + file, e);
                        }
                    }
                });
                LOG.debug(">> OK!");
                return true;
            }
        } catch (Exception e) {
            LOG.error("Unable to create and/or lock file: " + file, e);
        }
        LOG.debug(">> Failed!");
        return false;
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        setupApplicationStyle();
        File lockFile = new File(PROFILE, LOCK_FILE);
        if(lockInstance(lockFile)){
            // start Simple ToDo table
            SimpleTodoTable table = new SimpleTodoTable();
            if(!registerServer(table)){
                LOG.info("No port is avaliable, please specify an unused port in the configuration file.");
            }
        }else{
            if(!registerClient()){
                LOG.info("No port is avaliable, please specify an unused port in the configuration file.");
                JOptionPane.showMessageDialog(null, "Only one running instance is allowed!", "Duplicated Instances Error", JOptionPane.ERROR_MESSAGE);
            }
            LOG.info("Already Running another instance, exiting program ...");
            LOG.warn(".......................... System Halt ..........................");
        }
    }

    private static void setupApplicationStyle() {
        try {
            BeautyEyeLNFHelper.frameBorderStyle = FrameBorderStyle.translucencyAppleLike;
            BeautyEyeLNFHelper.translucencyAtFrameInactive = false;
            UIManager.put("RootPane.setupButtonVisible", false);
            BeautyEyeLNFHelper.launchBeautyEyeLNF();
        } catch (final Exception r) {
        }

        final Font font = new Font(ConfigUtil.get("font"), Font.PLAIN, 12);
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
    }

    private static boolean registerClient() {
        int offset = 0;
        boolean ported = false;
        while(!ported && offset < 10) {
            try {
                String jndi = extractJNDI(offset);
                LOG.debug("Trying port [" + getPort(offset) + "] ... ");
                IBringToFront hello = (IBringToFront) Naming.lookup(jndi);
                ported = hello.performAction();
                LOG.debug(">> Success!");
                LOG.info("BringToFront RMI Client performed action.");
            } catch (Exception e) {
                offset++;
                LOG.debug(">> Failed!");
                LOG.error("Remote Exception: ", e);
            }
        }
        return ported;
    }

    private static boolean registerServer(SimpleTodoTable table) {
        int offset = 0;
        boolean ported = false;
        while(!ported && offset < 10){
            try{
                int port = getPort(offset);
                LOG.debug("Trying port [" + port + "] ... ");
                LocateRegistry.createRegistry(port);
                ported = true;
                LOG.debug(">> Success!");
                try {
                    Naming.rebind(extractJNDI(offset), new BringToFrontImpl(table));
                    LOG.info("BringToFront RMI Server is ready.");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }catch(Exception e){
                offset++;
                LOG.debug(">> Failed!");
                LOG.error("Register Port Exception: ", e);
            }
        }
        return ported;
    }

    private static String extractJNDI(int offset) {
        String encodedHome = new BASE64Encoder().encode(USER_HOME.getBytes());
        String jndi = "//" + HOST + ":" + getPort(offset) + "/IBringToFront" + encodedHome;
        LOG.debug("JNDI NAME: " + jndi);
        return jndi;
    }

    private static int getPort(int offset) {
        int port = PORT + offset;
        return port;
    }
}
