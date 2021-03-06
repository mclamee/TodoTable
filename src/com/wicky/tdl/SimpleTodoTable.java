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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.WriteAbortedException;
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
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.FrameBorderStyle;
import org.jb2011.lnf.beautyeye.ch3_button.BEButtonUI;

import sun.misc.BASE64Encoder;

import com.wicky.tdl.data.DataVector;
import com.wicky.tdl.rmi.BringToFrontImpl;
import com.wicky.tdl.rmi.IBringToFront;
import com.wicky.tdl.util.ConfigUtil;
import com.wicky.tdl.util.JTableHelper;


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

    private TableRowSorter<TableModel> rowSorter;
    private JTextField jtfFilter;
    
    private SystemTray sysTray;
    private TrayIcon trayIcon;
    private Timer timer;
    
    private ObjectInputStream in;
    private ObjectOutputStream out;

    protected boolean dataChanged;
    protected boolean runing;

    public SimpleTodoTable() throws IOException {
        // 1. create data model
        dataModel = new SimpleTableModel();
        dataModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                dataChanged = true;
            }
        });
        
        // 2. load data vector from saved file
        dataFile = new File(PROFILE, DATA_FILE);
        if(dataFile.isDirectory())dataFile.delete();
        if(!dataFile.exists()){
            dataFile.createNewFile();
            LOG.debug("No data file exists, creating new file: ["+dataFile.getAbsolutePath()+"]");
        }
        DataVector data = null;
        try {
            LOG.debug("Reading data file ... ");
            in = new ObjectInputStream(new FileInputStream(dataFile));
            if(in != null){
                Object object = in.readObject();
                if(object instanceof DataVector){
                    data = (DataVector) object;
                    LOG.debug(">> Success!");
                }else if(object instanceof Vector<?>){
                    // old data found
                    LOG.debug("Old data detected, upgrading ...");
                    dataModel.initOldData((Vector<?>)object);
                    LOG.debug(">> Success!");
                }else{
                    LOG.debug(">> Canceled!");
                    LOG.error("Invalid Data Type.");
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOG.error("Try to close input stream: ", e);
                    }
                    try {
                        dataFile.delete();
                        dataFile.createNewFile();
                    } catch (IOException e) {
                        LOG.error("try to recreate data file: ", e);
                    }
                }
            }
        } catch (EOFException e) {
            LOG.debug(">> Canceled.");
            LOG.error("File is empty: ", e);
        } catch (ClassNotFoundException e) {
            LOG.debug(">> Canceled.");
            LOG.error("File format issue: ", e);
        } catch (NotSerializableException e){
            LOG.debug(">> Canceled.");
            LOG.error("Cannot load data: ", e);
        } catch (WriteAbortedException e){
            LOG.debug(">> Canceled.");
            LOG.error("Cannot load data: ", e);
        }finally{
            if(in != null){
                in.close();
            }
        }
        
        // 3. assemble data model
        if(data != null){
            dataModel.initData(data);
        }
        
        // 4. setup data model
        this.setModel(dataModel);
        
        // 5. adjust column width
        adjustColumnWidth();
        
        // 6. setup UI to support drag and drop rows
        this.setUI(new DragDropRowTableUI());
        
        // 7. add other components and bind events
        this.setSelectionBackground(Color.CYAN);
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
                    stopCellEditing();
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

        Dimension btnSize = new Dimension(150, 25);
        
        btnAdd = new JButton("Add New");
        btnAdd.setPreferredSize(btnSize);
        btnAdd.setUI(new BEButtonUI().setNormalColor(BEButtonUI.NormalColor.green));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD));
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = dataModel.addRow();
                stopCellEditing();
                jtfFilter.setText(null);
                refreshTable();
                ListSelectionModel model = SimpleTodoTable.this.getSelectionModel();
                model.clearSelection();
                model.setSelectionInterval(--row, row);
                JTableHelper.scrollToCenter(SimpleTodoTable.this, row, 1);
            }
        });

        btnClear = new JButton("Delete Finishied");
        btnClear.setPreferredSize(btnSize);
        btnClear.setForeground(Color.WHITE);
        btnClear.setFont(btnClear.getFont().deriveFont(Font.BOLD));
        btnClear.setUI(new BEButtonUI().setNormalColor(BEButtonUI.NormalColor.red));
        btnClear.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                stopCellEditing();
                jtfFilter.setText(null);
                int result = JOptionPane.showConfirmDialog(SimpleTodoTable.this, "Are you sure to delete all the finishied entries?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION){
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
        
        // setup row sorter and filters
        rowSorter = new TableRowSorter<>(this.getModel());
        rowSorter.setSortable(0, false);
        rowSorter.setSortable(1, false);
        rowSorter.setSortable(2, false);
        rowSorter.setSortable(3, false);
        this.setRowSorter(rowSorter);
        
        jtfFilter = new JTextField();
        jtfFilter.setForeground(Color.RED);
        jtfFilter.setFont(jtfFilter.getFont().deriveFont(Font.BOLD));
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("  Specify a word to search:   ");
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                jtfFilter.requestFocus();
                jtfFilter.selectAll();
            }
        });
        label.setToolTipText("Type anything here. Tip: Use \"true\" or \"false\" to filter entry status. ");
        panel.add(label, BorderLayout.WEST);
        panel.add(jtfFilter, BorderLayout.CENTER);
        panel.add(new JLabel(" "), BorderLayout.EAST);
        mainPan.add(panel, BorderLayout.SOUTH);
        jtfFilter.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                stopCellEditing();
            }
        });
        jtfFilter.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
                    jtfFilter.setText(null);
                }
            }
        });
        jtfFilter.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent e) {
                String text = jtfFilter.getText();

                if (text.trim().length() == 0) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                String text = jtfFilter.getText();

                if (text.trim().length() == 0) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        });
        
        frame = new JFrame(APP_NAME);
        frame.setIconImage(new ImageIcon(APP_LOGO).getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPan);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // setup system tray icon
        createTrayIcon();
        frame.addWindowListener(new WindowAdapter() {
            public void windowIconified(WindowEvent e) {
                minimizeToTray();
            }
        });
        
        // setup window close hook method
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
        
        // setup timer to support automatic save
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
                        stopCellEditing();
                        int rowCount = dataModel.getRowCount();
                        if(rowCount != 0){
                            int result = JOptionPane.showConfirmDialog(SimpleTodoTable.this, "Are you sure to delete this row?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                            if(result == JOptionPane.YES_OPTION){
                                dataModel.removeRow(selectedRow);
                                if(selectedRow < dataModel.getRowCount()){
                                    SimpleTodoTable.this.setRowSelectionInterval(selectedRow, selectedRow);
                                }
                                stopCellEditing();
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
            out.flush();
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
    

    public void createTrayIcon() {
        sysTray = SystemTray.getSystemTray();
        PopupMenu popupMenu = new PopupMenu();
        MenuItem mi = new MenuItem("Show");
        MenuItem exit = new MenuItem("Exit");
        popupMenu.add(mi);
        popupMenu.add(exit);

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
            sysTray.add(trayIcon);
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    frame.setVisible(false);
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

    private void adjustColumnWidth() {
        // Tweak the appearance of the table by manipulating its column model
        TableColumnModel colmodel = this.getColumnModel();
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // Set column widths
        colmodel.getColumn(0).setPreferredWidth(1);
        colmodel.getColumn(1).setPreferredWidth(320);
        colmodel.getColumn(2).setPreferredWidth(200);
        colmodel.getColumn(3).setPreferredWidth(30);
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
    
    private void stopCellEditing() {
        TableCellEditor ce = SimpleTodoTable.this.getCellEditor();
        if(ce != null){
            ce.stopCellEditing();
        }
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

        final Font font = new Font(ConfigUtil.get("font"), Font.PLAIN, 14);
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
