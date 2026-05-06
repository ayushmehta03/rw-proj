package ui;

import model.StockData;
import service.PatternDetector;
import service.YahooFinanceFetcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MainWindow extends JFrame {

    private static final Color BG_DARK    = new Color(0x0B0E14);
    private static final Color BG_PANEL   = new Color(0x12161E);
    private static final Color BG_CONTROL = new Color(0x1E222D);
    private static final Color ACCENT     = new Color(0x2962FF);
    private static final Color SUCCESS    = new Color(0x26A69A);
    private static final Color ACCENT2    = new Color(0xF5A623);
    private static final Color TEXT       = new Color(0xE6EDF3);
    private static final Color MUTED      = new Color(0x707A8A);
    private static final Color BORDER     = new Color(0x2B3139);

    private final JTextField   tickerField;
    private final JComboBox<String> intervalBox;
    private final JComboBox<String> periodBox;
    private final JButton      fetchBtn;
    private final JButton      exportBtn;
    private final JLabel       statusLabel;
    private final CandlestickChart chart;
    private final JTable       patternTable;
    private final DefaultListModel<String> patternListModel;
    private final JLabel       statsLabel;

    public MainWindow() {
        setTitle("Editorzzz Stock Analyser");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        controls.setBackground(BG_PANEL);
        controls.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        controls.add(styledLabel("SYMBOL"));
        tickerField = styledField("WIPRO.NS", 10);
        tickerField.addActionListener(e -> fetchData());
        controls.add(tickerField);

        controls.add(styledLabel("INTERVAL"));
        intervalBox = styledCombo(new String[]{"5m", "1m", "15m", "30m", "60m"});
        controls.add(intervalBox);

        controls.add(styledLabel("RANGE"));
        periodBox = styledCombo(new String[]{"1d", "5d", "1mo"});
        controls.add(periodBox);

        fetchBtn = accentBtn("Fetch Data", ACCENT);
        fetchBtn.addActionListener(e -> fetchData());
        controls.add(fetchBtn);

        exportBtn = accentBtn("Export Chart", BG_CONTROL);
        exportBtn.setForeground(TEXT);
        exportBtn.addActionListener(e -> exportChart());
        controls.add(exportBtn);

        statusLabel = new JLabel("System Ready");
        statusLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 20, 0, 0));
        controls.add(statusLabel);

        chart = new CandlestickChart();

        JScrollPane chartScroll = new JScrollPane(chart);
        chartScroll.setBorder(BorderFactory.createEmptyBorder());
        chartScroll.getViewport().setBackground(BG_DARK);
        chartScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel sidePanel = new JPanel(new BorderLayout(0, 10));
        sidePanel.setBackground(BG_PANEL);
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER),
            new EmptyBorder(20, 15, 15, 15)
        ));
        sidePanel.setPreferredSize(new Dimension(260, 0));

        JLabel sideTitle = new JLabel("PATTERN ANALYSIS");
        sideTitle.setFont(new Font("Inter", Font.BOLD, 11));
        sideTitle.setForeground(MUTED);
        sidePanel.add(sideTitle, BorderLayout.NORTH);

        patternListModel = new DefaultListModel<>();
        JList<String> patternList = new JList<>(patternListModel);
        patternList.setBackground(BG_PANEL);
        patternList.setForeground(TEXT);
        patternList.setFont(new Font("Inter", Font.PLAIN, 13));
        patternList.setSelectionBackground(new Color(0x1E222D));
        patternList.setCellRenderer(new PatternCellRenderer());

        JScrollPane listScroll = new JScrollPane(patternList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        listScroll.getViewport().setBackground(BG_PANEL);
        sidePanel.add(listScroll, BorderLayout.CENTER);

        statsLabel = new JLabel("<html></html>");
        statsLabel.setFont(new Font("Inter", Font.PLAIN, 13));
        statsLabel.setForeground(TEXT);
        statsLabel.setBorder(new EmptyBorder(15, 5, 5, 5));
        sidePanel.add(statsLabel, BorderLayout.SOUTH);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_PANEL);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        statusBar.setPreferredSize(new Dimension(0, 30));

        JLabel hint = new JLabel("  Market Data provided by Yahoo Finance API | Editorzzz Engine");
        hint.setFont(new Font("Inter", Font.PLAIN, 10));
        hint.setForeground(MUTED);
        statusBar.add(hint, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_DARK);
        center.add(chartScroll, BorderLayout.CENTER);
        center.add(sidePanel, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(controls, BorderLayout.NORTH);
        add(center,   BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        patternTable = null;
        chart.setData(null, "");
    }

    private void fetchData() {
        String ticker   = tickerField.getText().trim().toUpperCase();
        String interval = (String) intervalBox.getSelectedItem();
        String period   = (String) periodBox.getSelectedItem();

        if (ticker.isEmpty()) {
            showError("Please enter a ticker symbol.");
            return;
        }

        fetchBtn.setEnabled(false);
        statusLabel.setForeground(ACCENT2);
        statusLabel.setText("Connecting to server...");
        patternListModel.clear();

        SwingWorker<List<StockData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<StockData> doInBackground() throws Exception {
                List<StockData> data = YahooFinanceFetcher.fetch(ticker, interval, period);
                PatternDetector.detect(data);
                return data;
            }

            @Override
            protected void done() {
                try {
                    List<StockData> data = get();
                    chart.setData(data, ticker);
                    populateSidePanel(data, ticker);
                    statusLabel.setForeground(SUCCESS);
                    statusLabel.setText("● Live: " + ticker + " (" + interval + ")");
                } catch (Exception ex) {
                    statusLabel.setForeground(new Color(0xEF5350));
                    statusLabel.setText("Connection Failed");
                    showError("Could not fetch data:\n" + ex.getMessage());
                } finally {
                    fetchBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void populateSidePanel(List<StockData> data, String ticker) {
        patternListModel.clear();
        long doji = 0, hammer = 0, engulf = 0;
        for (StockData bar : data) {
            if (bar.getPattern() != null) {
                patternListModel.addElement(
                    bar.getDateTime().toLocalTime().toString().substring(0, 5)
                    + "  |  " + bar.getPattern());
                switch (bar.getPattern()) {
                    case "Doji"      -> doji++;
                    case "Hammer"    -> hammer++;
                    case "Engulfing" -> engulf++;
                }
            }
        }
        long bull = data.stream().filter(StockData::isBullish).count();
        long bear = data.size() - bull;
        statsLabel.setText("<html>"
            + "<div style='padding:10px; background:#1E222D; border-radius:5px;'>"
            + "<span style='color:#707A8A; font-size:10px;'>INSTRUMENT</span><br>"
            + "<b style='color:#FFFFFF; font-size:14px;'>" + ticker + "</b><br><br>"
            + "<span style='color:#26A69A'>Bullish Vol: " + bull + "</span><br>"
            + "<span style='color:#EF5350'>Bearish Vol: " + bear + "</span><br><br>"
            + "<hr color='#2B3139'><br>"
            + "<span style='color:#42A5F5'>Doji Detected: " + doji + "</span><br>"
            + "<span style='color:#66BB6A'>Hammer Signals: " + hammer + "</span><br>"
            + "<span style='color:#FFA726'>Engulfing Lines: " + engulf + "</span>"
            + "</div></html>");
    }

    private void exportChart() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(tickerField.getText().trim() + "_analysis.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            chart.getWidth(), chart.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        chart.paint(img.getGraphics());
        try {
            javax.imageio.ImageIO.write(img, "png", file);
            JOptionPane.showMessageDialog(this, "Analysis saved successfully.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font("Inter", Font.BOLD, 10));
        return l;
    }

    private JTextField styledField(String def, int cols) {
        JTextField f = new JTextField(def, cols);
        f.setBackground(BG_CONTROL);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(new Font("Inter", Font.BOLD, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(8, 12, 8, 12)
        ));
        return f;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(BG_CONTROL);
        cb.setForeground(TEXT);
        cb.setFont(new Font("Inter", Font.PLAIN, 13));
        cb.setBorder(BorderFactory.createLineBorder(BORDER));
        cb.setFocusable(false);
        return cb;
    }

    private JButton accentBtn(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Inter", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 35));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private void showError(String msg) {
        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("Panel.background", BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        JOptionPane.showMessageDialog(this, msg, "Market Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class PatternCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
            super.getListCellRendererComponent(l, v, i, sel, foc);
            String text = v.toString();
            setBorder(new EmptyBorder(8, 10, 8, 10));
            if (text.contains("Doji")) setForeground(new Color(0x42A5F5));
            else if (text.contains("Hammer")) setForeground(new Color(0x66BB6A));
            else if (text.contains("Engulfing")) setForeground(new Color(0xFFA726));
            else setForeground(TEXT);
            setBackground(sel ? new Color(0x1E222D) : (i % 2 == 0 ? BG_PANEL : new Color(0x161B22)));
            return this;
        }
    }
}