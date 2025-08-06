package vniiem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TLMMonitor {

    private static final int UDP_PORT = 15000;
    private static final int TLM_PACKET_SIZE = 26;

    private DatagramSocket socket;
    private volatile boolean running;
    private byte[] buffer = new byte[TLM_PACKET_SIZE];
    private Thread receiverThread;

    private JFrame frame;
    private JButton startStopButton;
    private DefaultTableModel tableModel;
    private JTable dataTable;
    private JLabel statusLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
            }
            new TLMMonitor().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        frame = new JFrame("TLM Monitor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        startStopButton = new JButton("Start");
        startStopButton.addActionListener(e -> toggleListening());
        topPanel.add(startStopButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearTable());
        topPanel.add(clearButton);

        statusLabel = new JLabel("Status: Stopped");
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);

        frame.add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"#", "Синхромаркер", "Счётчик пакетов", "Время", "Синус угла", "Контрольная сумма"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        dataTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowValid(row)) {
                    c.setBackground(Color.PINK);
                    c.setForeground(Color.BLACK);
                } else {
                    if (isCellSelected(row, column)) {
                        c.setBackground(Color.LIGHT_GRAY);
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(40); 
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        dataTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        JScrollPane tableScrollPane = new JScrollPane(dataTable);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        frame.add(tableScrollPane, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopListening();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void toggleListening() {
        if (running) {
            stopListening();
        } else {
            try {
                startListening();
            } catch (SocketException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Не удалось запустить прослушку: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    /**
     * @param status
     */
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + status));
    }


    public void startListening() throws SocketException {
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket(UDP_PORT);
            running = true;
            startStopButton.setText("Stop");
            updateStatus("Прослушивается порт " + UDP_PORT);
            
            receiverThread = new Thread(this::listenLoopUDP);
            receiverThread.setName("TLMReceiverThread");
            receiverThread.start();
        }
    }

    private void listenLoopUDP() {
        System.out.println("Поток приёма UDP запущен");
        
        int rowCount = 0;
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                
                byte[] actualData = new byte[TLM_PACKET_SIZE];
                System.arraycopy(packet.getData(), packet.getOffset(), actualData, 0, TLM_PACKET_SIZE);
                TlmData tlmData = parseTlmPacket(actualData);
                
                if (tlmData != null) {
                    final TlmData dataToDisplay = tlmData;
                    final int currentRow = ++rowCount;
                    SwingUtilities.invokeLater(() -> addRowToTable(currentRow, dataToDisplay));
                } else {
                    System.err.println("Ошибка");
                }
                
            } catch (IOException e) {
                if (socket != null && socket.isClosed()) {
                    running = false;
                    System.out.println("Сокет закрыт, поток приёма завершается");
                } else {
                    System.err.println("Ошибка приёма пакета: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Поток приёма UDP завершён");
        SwingUtilities.invokeLater(() -> {
            startStopButton.setText("Start");
            updateStatus("Stopped");
        });
    }

    public void stopListening() {
        boolean wasRunning = running;
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (wasRunning && receiverThread != null) {
            try {
                receiverThread.join(1000);
                if (receiverThread.isAlive()) {
                    System.err.println("Поток приёма не завершился корректно.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Ожидание завершения потока прервано.");
            }
        }
        if (wasRunning) {
            System.out.println("Прослушка остановлена");
        }
    }

    /**
     * @param data
     * @return
     */
    private TlmData parseTlmPacket(byte[] data) {
        if (data.length < TLM_PACKET_SIZE) {
            System.err.println("Неверный размер пакета: " + data.length);
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int syncMarker = buffer.getInt();
        boolean syncValid = (syncMarker == 0x12345678);

        int packetCounter = buffer.getInt();

        double timestamp = buffer.getDouble();

        double value = buffer.getDouble();

        int crcReceived = buffer.getShort() & 0xFFFF; 

        short crcCalculatedShort = vniiem.utils.Crc.crc16Ccitt(data, 0, TLM_PACKET_SIZE - 2);
        int crcCalculated = crcCalculatedShort & 0xFFFF; 
        boolean crcValid = (crcReceived == crcCalculated);

        return new TlmData(syncMarker, packetCounter, timestamp, value, crcReceived, crcValid, syncValid);
    }

    /**
     * @param rowNum
     * @param data
     */
    private void addRowToTable(int rowNum, TlmData data) {
        String syncMarkerStr = data.isSyncValid() ? 
            String.format("0x%08X", data.getSyncMarker()) : 
            String.format("0x%08X (!)", data.getSyncMarker());
            
        String counterStr = String.valueOf(data.getPacketCounter());
        String timeStr = String.format("%.6f", data.getTimestamp());
        String valueStr = String.format("%.6f", data.getValue());
        String crcStatusStr = data.isCrcValid() ? "OK" : "FAIL";

        Object[] row = {
            rowNum,
            syncMarkerStr,
            counterStr,
            timeStr,
            valueStr,
            crcStatusStr
        };

        tableModel.addRow(row);
        
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow >= 0) {
            dataTable.scrollRectToVisible(dataTable.getCellRect(lastRow, 0, true));
        }
    }
    
    /**
     * @param row
     * @return
     */
    private boolean isRowValid(int row) {
        String crcStatus = (String) tableModel.getValueAt(row, 5);
        String syncMarker = (String) tableModel.getValueAt(row, 1);
        return "OK".equals(crcStatus) && !syncMarker.contains("(!)");
    }
}