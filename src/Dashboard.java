import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** The desktop dashboard. UI code stays separate from scheduling/heap logic. */
public class Dashboard extends JFrame {
    private static final Color NAVY = new Color(13, 23, 43);
    private static final Color INK = new Color(27, 39, 61);
    private static final Color MUTED = new Color(105, 117, 137);
    private static final Color CANVAS = new Color(244, 247, 251);
    private static final Color CARD = Color.WHITE;
    private static final Color ACCENT = new Color(52, 102, 225);
    private static final Color GREEN = new Color(30, 149, 111);
    private static final Color BORDER = new Color(226, 232, 240);

    private final Scheduler scheduler = new Scheduler();
    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JComboBox<String> priorityBox = new JComboBox<String>(new String[] {"CRITICAL", "HIGH", "MEDIUM", "LOW"});
    private final JTextField arrivalField = new JTextField("0");
    private final JTextField burstField = new JTextField();
    private final DefaultTableModel jobsModel = nonEditableModel(new String[] {"JOB ID", "JOB NAME", "PRIORITY", "ARRIVAL", "BURST", "STATUS"});
    private final DefaultTableModel queueModel = nonEditableModel(new String[] {"RANK", "JOB", "PRIORITY", "STATUS"});
    private final DefaultTableModel resultsModel = nonEditableModel(new String[] {"JOB", "PRIORITY", "CT", "TAT", "WT", "RT"});
    private final JTable jobsTable = createTable(jobsModel);
    private final JTable queueTable = createTable(queueModel);
    private final JTable resultsTable = createTable(resultsModel);
    private final JLabel totalValue = metricValue();
    private final JLabel pendingValue = metricValue();
    private final JLabel completedValue = metricValue();
    private final JLabel currentValue = metricValue();
    private final JLabel avgWaitingValue = metricValue();
    private final JLabel avgTatValue = metricValue();
    private final JLabel avgResponseValue = metricValue();
    private final JLabel nextJobValue = new JLabel("No queued jobs");
    private final JLabel nextPriorityValue = new JLabel("—");
    private final GanttPanel ganttPanel = new GanttPanel();
    private Job selectedJob;

    public Dashboard() {
        setTitle("Priority Job Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1450, 900);
        setMinimumSize(new Dimension(1120, 720));
        setLocationRelativeTo(null);
        buildUi();
        addSamples();
        refreshDashboard();
    }

    private void buildUi() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(CANVAS);
        page.add(header(), BorderLayout.NORTH);
        JPanel content = new JPanel();
        content.setBackground(CANVAS);
        content.setBorder(new javax.swing.border.EmptyBorder(24, 30, 36, 30));
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(metrics());
        content.add(space(18));
        content.add(topContent());
        content.add(space(18));
        content.add(ganttCard());
        content.add(space(18));
        content.add(resultsCard());
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER);
        setContentPane(page);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new javax.swing.border.EmptyBorder(22, 30, 22, 30));
        JPanel title = new JPanel(); title.setOpaque(false); title.setLayout(new javax.swing.BoxLayout(title, javax.swing.BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("PRIORITY JOB SCHEDULER");
        heading.setForeground(Color.WHITE); heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel("CPU Job Scheduling using a Custom Priority Queue");
        subtitle.setForeground(new Color(186, 204, 232)); subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        title.add(heading); title.add(space(5)); title.add(subtitle);
        header.add(title, BorderLayout.WEST);
        JLabel badge = new JLabel("  CUSTOM HEAP  ");
        badge.setOpaque(true); badge.setBackground(new Color(34, 57, 92)); badge.setForeground(new Color(202, 220, 251));
        badge.setFont(new Font("SansSerif", Font.BOLD, 11)); badge.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        header.add(badge, BorderLayout.EAST);
        return header;
    }

    private JPanel metrics() {
        JPanel row = new JPanel(new java.awt.GridLayout(1, 7, 12, 0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        row.add(metricCard("TOTAL JOBS", totalValue, ACCENT));
        row.add(metricCard("PENDING", pendingValue, new Color(232, 133, 31)));
        row.add(metricCard("COMPLETED", completedValue, GREEN));
        row.add(metricCard("CURRENT JOB", currentValue, new Color(125, 89, 205)));
        row.add(metricCard("AVG WAIT", avgWaitingValue, new Color(21, 137, 180)));
        row.add(metricCard("AVG TURNAROUND", avgTatValue, new Color(13, 133, 108)));
        row.add(metricCard("AVG RESPONSE", avgResponseValue, new Color(190, 79, 118)));
        return row;
    }

    private JPanel metricCard(String label, JLabel value, Color stripe) {
        RoundedPanel card = new RoundedPanel(16, CARD);
        card.setLayout(new BorderLayout()); card.setBorder(BorderFactory.createEmptyBorder(13, 15, 12, 10));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel text = new JLabel(label); text.setForeground(MUTED); text.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel dot = new JLabel("●"); dot.setForeground(stripe); dot.setFont(new Font("SansSerif", Font.BOLD, 16));
        top.add(text, BorderLayout.WEST); top.add(dot, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH); card.add(value, BorderLayout.SOUTH);
        return card;
    }

    private JPanel topContent() {
        JPanel row = new JPanel(new java.awt.GridBagLayout()); row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 595));
        GridBagConstraints c = new GridBagConstraints(); c.gridy = 0; c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        c.gridx = 0; c.weightx = .60; c.insets = new Insets(0, 0, 0, 18); row.add(queueCard(), c);
        c.gridx = 1; c.weightx = .40; c.insets = new Insets(0, 0, 0, 0); row.add(managementCard(), c);
        return row;
    }

    private JPanel queueCard() {
        RoundedPanel card = card(); card.setLayout(new BorderLayout(0, 14));
        card.add(sectionTitle("PRIORITY JOB QUEUE", "Live ordering generated from the custom heap"), BorderLayout.NORTH);
        queueTable.getColumnModel().getColumn(0).setMaxWidth(60);
        queueTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        card.add(tableScroll(queueTable, 210), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(14, 0)); bottom.setOpaque(false);
        RoundedPanel next = new RoundedPanel(14, new Color(238, 244, 255)); next.setLayout(new BorderLayout()); next.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JPanel texts = new JPanel(); texts.setOpaque(false); texts.setLayout(new javax.swing.BoxLayout(texts, javax.swing.BoxLayout.Y_AXIS));
        JLabel overline = new JLabel("NEXT JOB  •  HEAP ROOT"); overline.setForeground(ACCENT); overline.setFont(new Font("SansSerif", Font.BOLD, 10));
        nextJobValue.setFont(new Font("SansSerif", Font.BOLD, 17)); nextJobValue.setForeground(INK);
        texts.add(overline); texts.add(space(5)); texts.add(nextJobValue); next.add(texts, BorderLayout.CENTER);
        JPanel priority = new JPanel(); priority.setOpaque(false); priority.setLayout(new javax.swing.BoxLayout(priority, javax.swing.BoxLayout.Y_AXIS));
        JLabel pl = new JLabel("PRIORITY"); pl.setForeground(MUTED); pl.setFont(new Font("SansSerif", Font.BOLD, 10));
        nextPriorityValue.setFont(new Font("SansSerif", Font.BOLD, 14)); priority.add(pl); priority.add(space(5)); priority.add(nextPriorityValue); next.add(priority, BorderLayout.EAST);
        bottom.add(next, BorderLayout.CENTER);
        JButton run = button("▶  RUN SCHEDULER", ACCENT); run.addActionListener(e -> runScheduler());
        run.setPreferredSize(new Dimension(205, 58)); bottom.add(run, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel managementCard() {
        RoundedPanel card = card(); card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("JOB MANAGEMENT", "Add, edit and manage scheduler inputs"), BorderLayout.NORTH);
        JPanel center = new JPanel(); center.setOpaque(false); center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));
        center.add(formPanel()); center.add(space(14));
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 3, 8, 0)); buttons.setOpaque(false); buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JButton add = button("Add Job", ACCENT); add.addActionListener(e -> addJob());
        JButton edit = outlineButton("Edit Job"); edit.addActionListener(e -> editJob());
        JButton clear = outlineButton("Clear Form"); clear.addActionListener(e -> clearForm());
        buttons.add(add); buttons.add(edit); buttons.add(clear); center.add(buttons); center.add(space(16));
        JLabel listLabel = new JLabel("ALL JOBS"); listLabel.setForeground(MUTED); listLabel.setFont(new Font("SansSerif", Font.BOLD, 10)); center.add(listLabel); center.add(space(7));
        jobsTable.getSelectionModel().addListSelectionListener(e -> chooseSelectedJob());
        center.add(tableScroll(jobsTable, 145));
        JPanel controls = new JPanel(new java.awt.GridLayout(1, 3, 8, 0)); controls.setOpaque(false); controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JButton remove = outlineButton("Remove Job"); remove.addActionListener(e -> removeJob());
        JButton reset = outlineButton("↻ Reset"); reset.addActionListener(e -> { scheduler.resetSchedule(); clearForm(); refreshDashboard(); });
        JButton clearAll = button("Clear All", new Color(185, 68, 83)); clearAll.addActionListener(e -> clearAll());
        controls.add(remove); controls.add(reset); controls.add(clearAll); center.add(space(10)); center.add(controls);
        card.add(center, BorderLayout.CENTER);
        card.add(algorithmFlow(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout()); form.setOpaque(false);
        addField(form, 0, 0, "JOB ID", idField); addField(form, 1, 0, "JOB NAME", nameField);
        addField(form, 0, 2, "PRIORITY", priorityBox); addField(form, 1, 2, "ARRIVAL TIME", arrivalField);
        addField(form, 0, 4, "BURST TIME", burstField);
        return form;
    }

    private void addField(JPanel panel, int col, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = col; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, col == 0 ? 0 : 8, 4, 0);
        JLabel l = new JLabel(label); l.setForeground(MUTED); l.setFont(new Font("SansSerif", Font.BOLD, 10)); panel.add(l, c);
        c.gridy++; c.insets = new Insets(0, col == 0 ? 0 : 8, 9, 0); field.setPreferredSize(new Dimension(100, 34));
        if (field instanceof JTextField) { ((JTextField) field).setFont(new Font("SansSerif", Font.PLAIN, 13)); ((JTextField) field).setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(4, 8, 4, 8))); }
        panel.add(field, c);
    }

    private JPanel ganttCard() {
        RoundedPanel card = card(); card.setLayout(new BorderLayout(0, 12)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        card.add(sectionTitle("GANTT CHART", "Actual execution sequence after running the scheduler"), BorderLayout.NORTH);
        ganttPanel.setPreferredSize(new Dimension(1000, 115)); card.add(ganttPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel resultsCard() {
        RoundedPanel card = card(); card.setLayout(new BorderLayout(0, 12)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        card.add(sectionTitle("SCHEDULING RESULTS", "Calculated from the executed schedule"), BorderLayout.NORTH);
        card.add(tableScroll(resultsTable, 170), BorderLayout.CENTER);
        return card;
    }

    private JPanel algorithmFlow() {
        RoundedPanel flow = new RoundedPanel(12, new Color(249, 250, 253)); flow.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 12)); flow.setLayout(new BorderLayout(0, 5));
        JLabel label = new JLabel("HOW THE CUSTOM QUEUE WORKS"); label.setForeground(MUTED); label.setFont(new Font("SansSerif", Font.BOLD, 9)); flow.add(label, BorderLayout.NORTH);
        JLabel steps = new JLabel("Jobs Added  →  Heap Queue  →  Highest Priority  →  Execute  →  Remove");
        steps.setForeground(INK); steps.setFont(new Font("SansSerif", Font.PLAIN, 11)); flow.add(steps, BorderLayout.CENTER);
        return flow;
    }

    private void addJob() {
        Job data = readForm(); if (data == null) return;
        if (scheduler.containsId(data.getId(), null)) { message("Job ID already exists. Please use a unique ID.", "Duplicate Job ID", JOptionPane.WARNING_MESSAGE); return; }
        scheduler.addJob(data); clearForm(); refreshDashboard();
    }

    private void editJob() {
        if (selectedJob == null) { message("Select a job in the All Jobs table before editing.", "No Job Selected", JOptionPane.INFORMATION_MESSAGE); return; }
        Job data = readForm(); if (data == null) return;
        if (scheduler.containsId(data.getId(), selectedJob)) { message("Job ID already exists. Please use a unique ID.", "Duplicate Job ID", JOptionPane.WARNING_MESSAGE); return; }
        if (!selectedJob.getId().equalsIgnoreCase(data.getId())) { message("Job ID cannot be changed while editing. Remove and add it again if needed.", "Job ID Fixed", JOptionPane.INFORMATION_MESSAGE); return; }
        selectedJob.update(data.getName(), data.getPriority(), data.getArrivalTime(), data.getBurstTime());
        scheduler.resetSchedule(); clearForm(); refreshDashboard();
    }

    private Job readForm() {
        String id = idField.getText().trim(); String name = nameField.getText().trim();
        if (id.isEmpty()) { message("Please enter a valid Job ID.", "Missing Job ID", JOptionPane.WARNING_MESSAGE); return null; }
        if (name.isEmpty()) { message("Please enter a Job Name.", "Missing Job Name", JOptionPane.WARNING_MESSAGE); return null; }
        try {
            int arrival = Integer.parseInt(arrivalField.getText().trim()); int burst = Integer.parseInt(burstField.getText().trim());
            if (arrival < 0) { message("Arrival Time cannot be negative.", "Invalid Arrival Time", JOptionPane.WARNING_MESSAGE); return null; }
            if (burst <= 0) { message("Burst Time must be greater than zero.", "Invalid Burst Time", JOptionPane.WARNING_MESSAGE); return null; }
            return new Job(id, name, priorityBox.getSelectedIndex() + 1, arrival, burst);
        } catch (NumberFormatException e) { message("Arrival Time and Burst Time must be whole numbers.", "Invalid Number", JOptionPane.WARNING_MESSAGE); return null; }
    }

    private void chooseSelectedJob() {
        int row = jobsTable.getSelectedRow(); if (row < 0 || row >= scheduler.getJobs().size()) return;
        String id = String.valueOf(jobsModel.getValueAt(row, 0));
        for (Job job : scheduler.getJobs()) if (job.getId().equals(id)) { selectedJob = job; break; }
        if (selectedJob != null) { idField.setText(selectedJob.getId()); nameField.setText(selectedJob.getName()); priorityBox.setSelectedIndex(selectedJob.getPriority() - 1); arrivalField.setText(String.valueOf(selectedJob.getArrivalTime())); burstField.setText(String.valueOf(selectedJob.getBurstTime())); }
    }

    private void removeJob() {
        if (selectedJob == null) { message("Select a job in the All Jobs table before removing it.", "No Job Selected", JOptionPane.INFORMATION_MESSAGE); return; }
        scheduler.removeJob(selectedJob); clearForm(); refreshDashboard();
    }

    private void clearAll() {
        if (scheduler.getJobs().isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Remove all jobs and scheduling results?", "Clear All Jobs", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) { scheduler.clearAll(); clearForm(); refreshDashboard(); }
    }

    private void runScheduler() {
        if (!scheduler.runScheduler()) { message("No jobs available for scheduling.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE); return; }
        clearForm(); refreshDashboard();
    }

    private void clearForm() { idField.setText(""); nameField.setText(""); priorityBox.setSelectedIndex(0); arrivalField.setText("0"); burstField.setText(""); selectedJob = null; jobsTable.clearSelection(); }

    private void refreshDashboard() {
        jobsModel.setRowCount(0); int completed = 0;
        for (Job job : scheduler.getJobs()) { jobsModel.addRow(new Object[] {job.getId(), job.getName(), Job.priorityLabel(job.getPriority()), job.getArrivalTime(), job.getBurstTime(), job.getStatus()}); if (Job.COMPLETED.equals(job.getStatus())) completed++; }
        queueModel.setRowCount(0); List<Job> queue = scheduler.createQueueSnapshot().orderedSnapshot();
        for (int i = 0; i < queue.size(); i++) { Job job = queue.get(i); queueModel.addRow(new Object[] {i + 1, job.getName(), Job.priorityLabel(job.getPriority()), job.getStatus()}); }
        Job next = queue.isEmpty() ? null : queue.get(0); nextJobValue.setText(next == null ? "No queued jobs" : next.getName()); nextPriorityValue.setText(next == null ? "—" : Job.priorityLabel(next.getPriority())); nextPriorityValue.setForeground(next == null ? MUTED : priorityColor(next.getPriority()));
        resultsModel.setRowCount(0); for (Job job : scheduler.getExecutionOrder()) resultsModel.addRow(new Object[] {job.getName(), Job.priorityLabel(job.getPriority()), job.getCompletionTime(), job.getTurnaroundTime(), job.getWaitingTime(), job.getResponseTime()});
        totalValue.setText(String.valueOf(scheduler.getJobs().size())); pendingValue.setText(String.valueOf(scheduler.getJobs().size() - completed)); completedValue.setText(String.valueOf(completed)); currentValue.setText(scheduler.getCurrentJobName());
        avgWaitingValue.setText(format(scheduler.averageWaiting())); avgTatValue.setText(format(scheduler.averageTurnaround())); avgResponseValue.setText(format(scheduler.averageResponse())); ganttPanel.repaint();
    }

    private String format(double value) { return String.format("%.1f", value); }
    private void message(String text, String title, int type) { JOptionPane.showMessageDialog(this, text, title, type); }
    private JLabel metricValue() { JLabel label = new JLabel("0"); label.setForeground(INK); label.setFont(new Font("SansSerif", Font.BOLD, 22)); return label; }
    private RoundedPanel card() { RoundedPanel panel = new RoundedPanel(16, CARD); panel.setBorder(BorderFactory.createEmptyBorder(19, 20, 19, 20)); return panel; }
    private JLabel sectionTitle(String title, String note) { JLabel label = new JLabel("<html><b>" + title + "</b><br/><span style='font-size:10px;color:#718096'>" + note + "</span></html>"); label.setForeground(INK); label.setFont(new Font("SansSerif", Font.PLAIN, 15)); return label; }
    private Component space(int height) { return javax.swing.Box.createRigidArea(new Dimension(1, height)); }
    private JScrollPane tableScroll(JTable table, int height) { JScrollPane pane = new JScrollPane(table); pane.setBorder(BorderFactory.createLineBorder(BORDER)); pane.setPreferredSize(new Dimension(100, height)); pane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height)); pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); return pane; }
    private DefaultTableModel nonEditableModel(String[] columns) { return new DefaultTableModel(columns, 0) { public boolean isCellEditable(int row, int column) { return false; } }; }
    private JTable createTable(DefaultTableModel model) { JTable table = new JTable(model); table.setRowHeight(30); table.setFont(new Font("SansSerif", Font.PLAIN, 12)); table.setForeground(INK); table.setSelectionBackground(new Color(224, 234, 255)); table.setSelectionForeground(INK); table.setGridColor(new Color(238, 241, 245)); table.setShowVerticalLines(false); table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10)); table.getTableHeader().setForeground(MUTED); table.getTableHeader().setBackground(new Color(249, 250, 252)); table.getTableHeader().setPreferredSize(new Dimension(1, 32)); table.setDefaultRenderer(Object.class, new CellRenderer()); return table; }
    private JButton button(String text, Color color) { JButton b = new JButton(text); b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true); b.setBackground(color); b.setForeground(Color.WHITE); b.setFont(new Font("SansSerif", Font.BOLD, 11)); b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)); return b; }
    private JButton outlineButton(String text) { JButton b = button(text, Color.WHITE); b.setForeground(INK); b.setBorder(BorderFactory.createLineBorder(BORDER)); return b; }
    private Color priorityColor(int p) { return p == 1 ? new Color(202, 53, 69) : p == 2 ? new Color(224, 123, 28) : p == 3 ? new Color(34, 130, 174) : new Color(91, 112, 142); }

    private void addSamples() {
        scheduler.addJob(new Job("J-101", "Server Failure", 1, 0, 4));
        scheduler.addJob(new Job("J-102", "Database Backup", 2, 1, 3));
        scheduler.addJob(new Job("J-103", "System Update", 2, 2, 3));
        scheduler.addJob(new Job("J-104", "Report Generation", 3, 3, 4));
        scheduler.addJob(new Job("J-105", "Data Cleanup", 4, 4, 2));
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            cell.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); cell.setHorizontalAlignment(col == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
            if (!selected && ("CRITICAL".equals(value) || "HIGH".equals(value) || "MEDIUM".equals(value) || "LOW".equals(value))) { cell.setForeground(priorityColor("CRITICAL".equals(value) ? 1 : "HIGH".equals(value) ? 2 : "MEDIUM".equals(value) ? 3 : 4)); cell.setFont(new Font("SansSerif", Font.BOLD, 11)); }
            else if (!selected && (Job.READY.equals(value) || Job.COMPLETED.equals(value))) { cell.setForeground(Job.COMPLETED.equals(value) ? GREEN : new Color(75, 104, 160)); cell.setFont(new Font("SansSerif", Font.BOLD, 10)); }
            else { cell.setFont(new Font("SansSerif", Font.PLAIN, 12)); if (!selected) cell.setForeground(INK); }
            return cell;
        }
    }

    private class GanttPanel extends JPanel {
        GanttPanel() { setOpaque(false); }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g = (Graphics2D) graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            List<Job> order = scheduler.getExecutionOrder(); int w = getWidth();
            if (order.isEmpty()) { g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.drawString("Run the scheduler to generate the execution timeline.", 18, 55); g.dispose(); return; }
            int left = 18, top = 25, available = w - 36, totalDuration = order.get(order.size() - 1).getCompletionTime() - order.get(0).getStartTime();
            int currentX = left; for (Job job : order) { int segment = Math.max(58, (int) ((double) job.getBurstTime() / totalDuration * available)); if (job == order.get(order.size() - 1)) segment = left + available - currentX;
                g.setPaint(new GradientPaint(currentX, top, priorityColor(job.getPriority()), currentX + segment, top + 34, priorityColor(job.getPriority()).brighter())); g.fillRoundRect(currentX, top, segment - 3, 34, 8, 8);
                g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 11)); String name = job.getName().length() > 17 ? job.getName().substring(0, 15) + "…" : job.getName(); g.drawString(name, currentX + 10, top + 21);
                g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 10)); g.drawString(String.valueOf(job.getStartTime()), currentX, top + 54); currentX += segment;
            }
            g.setColor(MUTED); g.setFont(new Font("SansSerif", Font.PLAIN, 10)); g.drawString(String.valueOf(order.get(order.size() - 1).getCompletionTime()), currentX - 4, top + 54); g.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius; private final Color background;
        RoundedPanel(int radius, Color background) { this.radius = radius; this.background = background; setOpaque(false); }
        protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(background); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g2.dispose(); super.paintComponent(g); }
    }
}
