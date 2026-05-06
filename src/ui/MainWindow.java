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

    private static final Color BG_DARK    = new Color(0x0D1117);
    private static final Color BG_PANEL   = new Color(0x161B22);
    private static final Color BG_CONTROL = new Color(0x21262D);
    private static final Color ACCENT     = new Color(0x26A69A);
    private static final Color ACCENT2    = new Color(0xF5A623);
    private static final Color TEXT       = new Color(0xE6EDF3);
    private static final Color MUTED      = new Color(0x8B949E);
    private static final Color BORDER     = new Color(0x30363D);

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
        setTitle("Stock Candlestick Analyser");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controls.setBackground(BG_PANEL);
        controls.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        controls.add(styledLabel("Ticker:"));

        tickerField = styledField("WIPRO.NS", 120);
        tickerField.addActionListener(e -> fetchData()); // Enter key
        controls.add(tickerField);

        controls.add(styledLabel("Interval:"));
        intervalBox = styledCombo(new String[]{"15m", "1m", "5m", "30m", "60m"});
        controls.add(intervalBox);

        controls.add(styledLabel("Period:"));
        periodBox = styledCombo(new String[]{"1d", "5d", "1mo"});
        controls.add(periodBox);

        fetchBtn = accentBtn("▶  Fetch", ACCENT);
        fetchBtn.addActionListener(e -> fetchData());
        controls.add(fetchBtn);

        exportBtn = accentBtn("⬇  Export PNG", BG_CONTROL);
        exportBtn.setForeground(MUTED);
        exportBtn.addActionListener(e -> exportChart());
        controls.add(exportBtn);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 15, 0, 0));
        controls.add(statusLabel);

        chart = new CandlestickChart();

        JScrollPane chartScroll = new JScrollPane(chart);
        chartScroll.setBorder(BorderFactory.createEmptyBorder());
        chartScroll.getViewport().setBackground(new Color(0x12161E));
        chartScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel sidePanel = new JPanel(new BorderLayout(0, 8));
        sidePanel.setBackground(BG_PANEL);
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER),
            new EmptyBorder(12, 10, 10, 10)
        ));
        sidePanel.setPreferredSize(new Dimension(210, 0));

        JLabel sideTitle = styledLabel("Detected Patterns");
        sideTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        sideTitle.setForeground(TEXT);
        sidePanel.add(sideTitle, BorderLayout.NORTH);

        patternListModel = new DefaultListModel<>();
        JList<String> patternList = new JList<>(patternListModel);
        patternList.setBackground(BG_CONTROL);
        patternList.setForeground(TEXT);
        patternList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        patternList.setSelectionBackground(ACCENT.darker());
        patternList.setBorder(new EmptyBorder(4, 6, 4, 6));
        patternList.setCellRenderer(new PatternCellRenderer());

        JScrollPane listScroll = new JScrollPane(patternList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        sidePanel.add(listScroll, BorderLayout.CENTER);

        statsLabel = new JLabel("<html></html>");
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statsLabel.setForeground(MUTED);
        statsLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        sidePanel.add(statsLabel, BorderLayout.SOUTH);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_PANEL);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        JLabel hint = new JLabel(
            "  Patterns: \u25cf Doji (blue)   \u25cf Hammer (green)   \u25cf Engulfing (orange)");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(MUTED);
        statusBar.add(hint, BorderLayout.WEST);
        JLabel credit = new JLabel("Yahoo Finance  |  Java Swing  ");
        credit.setFont(new Font("SansSerif", Font.PLAIN, 11));
        credit.setForeground(BORDER);
        statusBar.add(credit, BorderLayout.EAST);

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
        statusLabel.setText("Fetching " + ticker + "…");
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
                    statusLabel.setForeground(ACCENT);
                    statusLabel.setText(data.size() + " bars loaded  (" + interval + ", " + period + ")");
                } catch (Exception ex) {
                    statusLabel.setForeground(new Color(0xEF5350));
                    statusLabel.setText("Error: " + ex.getMessage());
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
                    + "  " + bar.getPattern());
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
            + "<b style='color:#E6EDF3'>" + ticker + "</b><br><br>"
            + "<span style='color:#8B949E'>Bars: " + data.size() + "</span><br>"
            + "<span style='color:#26A69A'>▲ Bullish: " + bull + "</span><br>"
            + "<span style='color:#EF5350'>▼ Bearish: " + bear + "</span><br><br>"
            + "<span style='color:#42A5F5'>Doji: " + doji + "</span><br>"
            + "<span style='color:#66BB6A'>Hammer: " + hammer + "</span><br>"
            + "<span style='color:#FFA726'>Engulfing: " + engulf + "</span>"
            + "</html>");
    }

    private void exportChart() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(
            tickerField.getText().trim() + "_chart.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            chart.getWidth(), chart.getHeight(),
            java.awt.image.BufferedImage.TYPE_INT_RGB);
        chart.paint(img.getGraphics());
        try {
            javax.imageio.ImageIO.write(img, "png", file);
            JOptionPane.showMessageDialog(this,
                "Chart saved to:\n" + file.getAbsolutePath(), "Exported",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return l;
    }

    private JTextField styledField(String def, int cols) {
        JTextField f = new JTextField(def, cols);
        f.setBackground(BG_CONTROL);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(new Font("Monospaced", Font.BOLD, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        return f;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(BG_CONTROL);
        cb.setForeground(TEXT);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.setBorder(BorderFactory.createLineBorder(BORDER));
        cb.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v,
                    int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                setBackground(sel ? ACCENT.darker() : BG_CONTROL);
                setForeground(TEXT);
                setBorder(new EmptyBorder(3, 8, 3, 8));
                return this;
            }
        });
        return cb;
    }

    private JButton accentBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(bg.equals(BG_CONTROL) ? MUTED : Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.equals(BG_CONTROL) ? BORDER : ACCENT.darker()),
            new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class PatternCellRenderer extends DefaultListCellRenderer {
        private static final Color D = new Color(0x42A5F5);
        private static final Color H = new Color(0x66BB6A);
        private static final Color E = new Color(0xFFA726);
        private static final Color DEF = new Color(0xE6EDF3);

        @Override
        public Component getListCellRendererComponent(JList<?> l, Object v,
                int i, boolean sel, boolean foc) {
            super.getListCellRendererComponent(l, v, i, sel, foc);
            String text = v.toString();
            setFont(new Font("Monospaced", Font.PLAIN, 12));
            setBackground(sel ? new Color(0x1E4A58) : (i % 2 == 0 ? new Color(0x21262D) : new Color(0x1C2128)));
            if (text.contains("Doji"))      setForeground(D);
            else if (text.contains("Hammer")) setForeground(H);
            else if (text.contains("Engulfing")) setForeground(E);
            else setForeground(DEF);
            setBorder(new EmptyBorder(3, 8, 3, 8));
            return this;
        }
    }
}
