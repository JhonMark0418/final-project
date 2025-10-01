import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * HotelReservationSystem.java
 * Simple, easy-to-use hotel reservation GUI (single-file).
 * Features:
 * - Create reservations (auto-assigns an available room of requested type)
 * - View/search reservations
 * - Cancel, Check-in, Check-out reservations
 * - In-memory storage, no external dependencies
 *
 * Run: javac HotelReservationSystem.java && java HotelReservationSystem
 */
public class HotelReservationSystem extends JFrame {
    private DefaultTableModel tableModel;
    private JTable reservationsTable;
    private List<Room> rooms = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();
    private int nextReservationId = 1001;
    // login / session
    private JTextArea statusArea;
    private Map<String, String> users = new HashMap<>();
    private Map<String, String> roles = new HashMap<>();
    private String currentUser = "";
    private String currentRole = "";
    // DateTimeFormatter no longer needed because date spinners provide LocalDate
    // conversion

    public HotelReservationSystem() {
        setTitle("Hotel Reservation System");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUsers();
        initRooms();
        initUI();

        // show login before fully exposing the UI
        showLoginDialog();

        setVisible(true);
    }

    private void initRooms() {
        // Add sample rooms (roomNumber, type)
        for (int i = 101; i <= 105; i++)
            rooms.add(new Room(i, "Single"));
        for (int i = 201; i <= 206; i++)
            rooms.add(new Room(i, "Double"));
        for (int i = 301; i <= 303; i++)
            rooms.add(new Room(i, "Suite"));
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Minimal color palette
        Color BG = new Color(250, 252, 254); // page background
        Color ACCENT = new Color(33, 150, 243); // primary action
        Color MUTED = new Color(245, 245, 245); // muted buttons / panels
        Color TEXT = Color.BLACK;

        getContentPane().setBackground(BG);

        // Header + Top toolbar
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(BG);

        JLabel header = new JLabel("Hotel Reservation");
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.setForeground(ACCENT.darker());
        northPanel.add(header, BorderLayout.NORTH);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setBackground(BG);

        JButton newResBtn = new JButton("New Reservation");
        JButton checkInBtn = new JButton("Check In");
        JButton checkOutBtn = new JButton("Check Out");
        JButton cancelBtn = new JButton("Cancel Reservation");
        JButton refreshBtn = new JButton("Refresh");
        JButton logoutBtn = new JButton("Logout");
        JTextField searchField = new JTextField(20);
        // placeholder text for search field
        final String searchPlaceholder = "Search by name, ID, room, type or date (yyyy-MM-dd)";
        searchField.setForeground(Color.GRAY);
        searchField.setText(searchPlaceholder);
        searchField.setToolTipText("Type and press Enter or click Search");
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals(searchPlaceholder)) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setForeground(Color.GRAY);
                    searchField.setText(searchPlaceholder);
                }
            }
        });
        // Make texts black and subtle style; highlight primary actions
        Font btnFont = new Font("SansSerif", Font.PLAIN, 12);
        for (JButton b : new JButton[] { checkInBtn, checkOutBtn, cancelBtn, refreshBtn, logoutBtn }) {
            b.setBackground(MUTED);
            b.setForeground(TEXT);
            b.setFocusPainted(false);
            b.setFont(btnFont);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            b.setOpaque(true);
        }
        // Primary actions use accent (white text)
        newResBtn.setBackground(ACCENT);
        newResBtn.setForeground(Color.WHITE);
        newResBtn.setFocusPainted(false);
        newResBtn.setFont(btnFont.deriveFont(Font.BOLD));
        newResBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newResBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        newResBtn.setOpaque(true);

        top.add(newResBtn);
        top.add(checkInBtn);
        top.add(checkOutBtn);
        top.add(cancelBtn);
        top.add(refreshBtn);
        top.add(logoutBtn);
        top.add(Box.createHorizontalStrut(20));
        top.add(new JLabel("Search:"));
        top.add(searchField);

        northPanel.add(top, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Table
        String[] cols = { "ID", "Guest", "Room", "Type", "Check-In", "Check-Out", "Status", "Action" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reservationsTable = new JTable(tableModel);
        reservationsTable.setRowHeight(30);
        reservationsTable.getTableHeader().setBackground(new Color(240, 243, 247));
        reservationsTable.getTableHeader().setForeground(TEXT);
        reservationsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        reservationsTable.setSelectionBackground(new Color(204, 232, 255));
        reservationsTable.setSelectionForeground(Color.BLACK);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // align ID and Room columns to center and set preferred widths
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        if (reservationsTable.getColumnModel().getColumnCount() >= 4) {
            reservationsTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
            reservationsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Room
            reservationsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
            reservationsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            reservationsTable.getColumnModel().getColumn(7).setPreferredWidth(80); // Action
        }

        // subtle row striping and status-based highlighting
        reservationsTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                    return c;
                }

                // default subtle stripe
                Color base = (row % 2 == 0) ? Color.WHITE : new Color(249, 250, 252);

                // try to read status from the model (last column)
                String statusVal = "";
                try {
                    Object s = table.getModel().getValueAt(row, 6);
                    if (s != null)
                        statusVal = s.toString();
                } catch (Exception ignore) {
                }

                // status-based colors
                if (statusVal.equalsIgnoreCase("Reserved")) {
                    // warning (light orange)
                    base = new Color(255, 244, 229);
                } else if (statusVal.equalsIgnoreCase("Checked-in") || statusVal.equalsIgnoreCase("Checked in")) {
                    // green for checked in
                    base = new Color(216, 255, 220);
                } else if (statusVal.equalsIgnoreCase("Checked-out") || statusVal.equalsIgnoreCase("Checked out")) {
                    // yellow for checked out
                    base = new Color(255, 249, 196);
                } else if (statusVal.equalsIgnoreCase("Cancelled") || statusVal.equalsIgnoreCase("Canceled")) {
                    // red-ish for cancelled
                    base = new Color(255, 224, 224);
                }

                c.setBackground(base);
                c.setForeground(TEXT);
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(reservationsTable);
        add(scroll, BorderLayout.CENTER);

        // set up Action column renderer/editor (last column)
        int actionCol = reservationsTable.getColumnCount() - 1;
        reservationsTable.getColumnModel().getColumn(actionCol).setCellRenderer(new ButtonRenderer());
        reservationsTable.getColumnModel().getColumn(actionCol).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Bottom status (kept as a field so other methods can update it)
        statusArea = new JTextArea(3, 40);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setBackground(new Color(255, 255, 255, 230));
        statusArea.setForeground(TEXT);
        statusArea.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)), "Status"));
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        // Wire actions
        newResBtn.addActionListener(e -> showNewReservationDialog(statusArea));
        refreshBtn.addActionListener(e -> refreshTable());
        // make search trigger on button click or Enter in the search field
        Runnable doSearch = () -> {
            String q = searchField.getText();
            if (q == null)
                q = "";
            q = q.trim();
            // treat placeholder as empty
            if (q.equals(searchPlaceholder) || q.isEmpty()) {
                refreshTable();
            } else {
                searchReservations(q);
            }
        };
        searchField.addActionListener(e -> doSearch.run());

        // Logout action: confirm then exit or cancel
        logoutBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Do you really want to log out?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                // inform and exit
                statusArea.setText("logged out successfully");
                JOptionPane.showMessageDialog(this, "logged out successfully");
                System.exit(0);
            } else {
                statusArea.setText("Logout cancelled");
            }
        });

        checkInBtn.addActionListener(e -> {
            int r = reservationsTable.getSelectedRow();
            if (r == -1) {
                statusArea.setText("Select a reservation to check in.");
                return;
            }
            int id = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
            checkInReservation(id, statusArea);
        });

        checkOutBtn.addActionListener(e -> {
            int r = reservationsTable.getSelectedRow();
            if (r == -1) {
                statusArea.setText("Select a reservation to check out.");
                return;
            }
            int id = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
            checkOutReservation(id, statusArea);
        });

        cancelBtn.addActionListener(e -> {
            int r = reservationsTable.getSelectedRow();
            if (r == -1) {
                statusArea.setText("Select a reservation to cancel.");
                return;
            }
            int id = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
            cancelReservation(id, statusArea);
        });

        // initial refresh
        refreshTable();
    }

    private void showNewReservationDialog(JTextArea status) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField guestField = new JTextField(20);
        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Single", "Double", "Suite" });
        // replace manual date input with date spinners
        SpinnerDateModel smIn = new SpinnerDateModel();
        JSpinner checkInSpinner = new JSpinner(smIn);
        checkInSpinner.setEditor(new JSpinner.DateEditor(checkInSpinner, "yyyy-MM-dd"));

        SpinnerDateModel smOut = new SpinnerDateModel();
        JSpinner checkOutSpinner = new JSpinner(smOut);
        checkOutSpinner.setEditor(new JSpinner.DateEditor(checkOutSpinner, "yyyy-MM-dd"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Guest name:"), gbc);
        gbc.gridx = 1;
        panel.add(guestField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Room type:"), gbc);
        gbc.gridx = 1;
        panel.add(typeBox, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Check-in:"), gbc);
        gbc.gridx = 1;
        panel.add(checkInSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Check-out:"), gbc);
        gbc.gridx = 1;
        panel.add(checkOutSpinner, gbc);

        int res = JOptionPane.showConfirmDialog(this, panel, "New Reservation", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION)
            return;

        String guest = guestField.getText().trim();
        String type = (String) typeBox.getSelectedItem();

        // read dates from spinners and convert to LocalDate
        Date inDate = (Date) checkInSpinner.getValue();
        Date outDate = (Date) checkOutSpinner.getValue();
        LocalDate cin = inDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate cout = outDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (guest.isEmpty()) {
            status.setText("Please fill guest name for reservation.");
            return;
        }

        try {
            // validate dates
            if (!cout.isAfter(cin)) {
                status.setText("Check-out date must be after check-in date.");
                return;
            }

            // find available room of requested type
            Room room = findAvailableRoom(type, cin, cout);
            if (room == null) {
                status.setText("No available " + type + " rooms for the selected dates.");
                return;
            }

            Reservation r = new Reservation(nextReservationId++, guest, room.number, type, cin, cout);
            reservations.add(r);
            status.setText("Reservation created. ID: " + r.id + " Room: " + room.number);
            refreshTable();
        } catch (DateTimeParseException ex) {
            status.setText("Invalid date format. Use yyyy-MM-dd.");
        }
    }

    // Initialize default users
    private void initUsers() {
        // default manager
        users.put("admin", "admin");
        roles.put("admin", "manager");
        // sample staff
        users.put("staff", "staff");
        roles.put("staff", "staff");
    }

    // Simple login dialog for staff/manager
    private void showLoginDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);

        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        p.add(userField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        p.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        p.add(passField, gbc);

        int r = JOptionPane.showConfirmDialog(this, p, "Login", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            System.exit(0);
            return;
        }

        String u = userField.getText().trim();
        String pw = new String(passField.getPassword());
        if (users.containsKey(u) && users.get(u).equals(pw)) {
            currentUser = u;
            currentRole = roles.getOrDefault(u, "staff");
            statusArea.setText("Logged in as: " + currentUser + " (" + currentRole + ")");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials", "Login failed", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();
        }
    }

    private Room findAvailableRoom(String type, LocalDate in, LocalDate out) {
        for (Room room : rooms) {
            if (!room.type.equals(type))
                continue;
            boolean available = true;
            for (Reservation r : reservations) {
                if (r.roomNumber == room.number && r.status.equals("Reserved")) {
                    // overlap check
                    if (!(out.isBefore(r.checkIn) || in.isAfter(r.checkOut.minusDays(1)))) {
                        available = false;
                        break;
                    }
                }
            }
            if (available)
                return room;
        }
        return null;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Reservation r : reservations) {
            tableModel.addRow(new Object[] { r.id, r.guest, r.roomNumber, r.type, r.checkIn.toString(),
                    r.checkOut.toString(), r.status, "View" });
        }
    }

    private void searchReservations(String q) {
        tableModel.setRowCount(0);
        String L = q.toLowerCase();
        for (Reservation r : reservations) {
            String hay = ("" + r.id + " " + r.guest + " " + r.roomNumber + " " + r.type + " " + r.status + " "
                    + r.checkIn.toString() + " " + r.checkOut.toString()).toLowerCase();
            if (hay.contains(L)) {
                tableModel.addRow(new Object[] { r.id, r.guest, r.roomNumber, r.type, r.checkIn.toString(),
                        r.checkOut.toString(), r.status, "View" });
            }
        }
    }

    private void checkInReservation(int id, JTextArea status) {
        Reservation r = findReservationById(id);
        if (r == null) {
            status.setText("Reservation not found.");
            return;
        }
        if (!r.status.equals("Reserved")) {
            status.setText("Only reserved bookings can be checked in.");
            return;
        }
        r.status = "Checked-in";
        status.setText("Checked in reservation " + id + " (Room " + r.roomNumber + ")");
        refreshTable();
    }

    private void checkOutReservation(int id, JTextArea status) {
        Reservation r = findReservationById(id);
        if (r == null) {
            status.setText("Reservation not found.");
            return;
        }
        if (!r.status.equals("Checked-in")) {
            status.setText("Only checked-in bookings can be checked out.");
            return;
        }
        r.status = "Checked-out";
        status.setText("Checked out reservation " + id + " (Room " + r.roomNumber + ")");
        refreshTable();
    }

    private void cancelReservation(int id, JTextArea status) {
        Reservation r = findReservationById(id);
        if (r == null) {
            status.setText("Reservation not found.");
            return;
        }
        if (r.status.equals("Checked-in")) {
            status.setText("Cannot cancel a checked-in reservation.");
            return;
        }
        r.status = "Cancelled";
        status.setText("Cancelled reservation " + id);
        refreshTable();
    }

    private Reservation findReservationById(int id) {
        for (Reservation r : reservations)
            if (r.id == id)
                return r;
        return null;
    }

    // Inner classes
    private static class Room {
        int number;
        String type;

        Room(int n, String t) {
            number = n;
            type = t;
        }
    }

    private static class Reservation {
        int id;
        String guest;
        int roomNumber;
        String type;
        LocalDate checkIn, checkOut;
        String status;

        Reservation(int id, String guest, int roomNumber, String type, LocalDate checkIn, LocalDate checkOut) {
            this.id = id;
            this.guest = guest;
            this.roomNumber = roomNumber;
            this.type = type;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.status = "Reserved";
        }
    }

    // Renderer for action button
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    // Editor for action button
    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = reservationsTable.getSelectedRow();
                if (row >= 0) {
                    int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                    Reservation r = findReservationById(id);
                    if (r != null) {
                        String msg = "ID: " + r.id + "\nGuest: " + r.guest + "\nRoom: " + r.roomNumber +
                                "\nType: " + r.type + "\nCheck-in: " + r.checkIn + "\nCheck-out: " + r.checkOut
                                + "\nStatus: " + r.status;
                        JOptionPane.showMessageDialog(HotelReservationSystem.this, msg, "Reservation Details",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HotelReservationSystem::new);
    }
}
