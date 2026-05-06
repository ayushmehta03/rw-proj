package ui;

import model.StockData;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CandlestickChart extends JPanel {

private static final Color BG          = new Color(0x12161E);
private static final Color GRID        = new Color(0x232A35);
private static final Color BULL        = new Color(0x26A69A);   // teal-green
private static final Color BEAR        = new Color(0xEF5350);   // red
private static final Color AXIS_TEXT   = new Color(0xB0BEC5);
private static final Color AXIS_LINE   = new Color(0x37474F);

private static final Color LABEL_BG    = new Color(28, 34, 48, 220);

    private static final Map<String, Color> PATTERN_COLORS = new HashMap<>();
    static {
        PATTERN_COLORS.put("Doji",      new Color(0x42A5F5)); // blue
        PATTERN_COLORS.put("Hammer",    new Color(0x66BB6A)); // green
        PATTERN_COLORS.put("Engulfing", new Color(0xFFA726)); // orange
    }

    private static final int PAD_LEFT   = 70;
    private static final int PAD_RIGHT  = 20;
    private static final int PAD_TOP    = 30;
    private static final int PAD_BOTTOM = 65;

    private List<StockData> data;
    private String ticker = "";

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("MM/dd HH:mm");

    public CandlestickChart() {
        setBackground(BG);
        setPreferredSize(new Dimension(900, 520));
    }

    public void setData(List<StockData> data, String ticker) {
        this.data   = data;
        this.ticker = ticker;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.isEmpty()) {
            drawEmpty(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();
        int chartW = W - PAD_LEFT - PAD_RIGHT;
        int chartH = H - PAD_TOP  - PAD_BOTTOM;

        double minP = data.stream().mapToDouble(StockData::getLow).min().orElse(0);
        double maxP = data.stream().mapToDouble(StockData::getHigh).max().orElse(1);
        double pad  = (maxP - minP) * 0.05;
        minP -= pad;  maxP += pad;
        double priceRange = maxP - minP;

        int n = data.size();
        double slotW = (double) chartW / n;
        double candleW = Math.max(2, slotW * 0.65);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        int yTicks = 6;
        for (int i = 0; i <= yTicks; i++) {
            double price = minP + priceRange * i / yTicks;
            int y = PAD_TOP + chartH - (int)(chartH * (price - minP) / priceRange);

            g2.setColor(GRID);
            g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 1f, new float[]{4, 4}, 0));
            g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);

            g2.setColor(AXIS_TEXT);
            g2.setStroke(new BasicStroke(1));
            String label = String.format("%.2f", price);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, PAD_LEFT - fm.stringWidth(label) - 5, y + 4);
        }

        g2.setColor(AXIS_LINE);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
        g2.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);

        for (int i = 0; i < n; i++) {
            StockData bar = data.get(i);
            double xCenter = PAD_LEFT + i * slotW + slotW / 2.0;

            int yHigh  = priceToY(bar.getHigh(),  minP, priceRange, chartH);
            int yLow   = priceToY(bar.getLow(),   minP, priceRange, chartH);
            int yOpen  = priceToY(bar.getOpen(),  minP, priceRange, chartH);
            int yClose = priceToY(bar.getClose(), minP, priceRange, chartH);

            Color c = bar.isBullish() ? BULL : BEAR;
            g2.setColor(c);
            g2.setStroke(new BasicStroke(1.2f));

            g2.drawLine((int) xCenter, yHigh, (int) xCenter, yLow);

            int bodyTop = Math.min(yOpen, yClose);
            int bodyH   = Math.max(1, Math.abs(yClose - yOpen));
            int bodyX   = (int)(xCenter - candleW / 2);

            g2.fillRect(bodyX, bodyTop, (int) candleW, bodyH);

            if (bodyH <= 1) {
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(bodyX, yClose, bodyX + (int) candleW, yClose);
            }
        }

        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.setColor(AXIS_TEXT);
        int labelEvery = Math.max(1, n / 10);
        for (int i = 0; i < n; i += labelEvery) {
            double xCenter = PAD_LEFT + i * slotW + slotW / 2.0;
            String ts = data.get(i).getDateTime().format(
                    data.size() > 40 ? TIME_FMT : DT_FMT);
            AffineTransform old = g2.getTransform();
            g2.translate((int) xCenter, PAD_TOP + chartH + 14);
            g2.rotate(Math.toRadians(-40));
            g2.drawString(ts, 0, 0);
            g2.setTransform(old);
        }

        int markerR = 5;
        Map<String, Boolean> legendDrawn = new HashMap<>();
        for (int i = 0; i < n; i++) {
            StockData bar = data.get(i);
            if (bar.getPattern() == null) continue;

            double xCenter = PAD_LEFT + i * slotW + slotW / 2.0;
            int yClose = priceToY(bar.getClose(), minP, priceRange, chartH);

            Color pc = PATTERN_COLORS.getOrDefault(bar.getPattern(), Color.YELLOW);
            g2.setColor(pc);
            g2.setStroke(new BasicStroke(1.5f));
            int mx = (int) xCenter - markerR;
            int my = yClose - markerR * 2 - markerR; // above the candle
            g2.fillOval(mx, my, markerR * 2, markerR * 2);
            g2.setColor(pc.darker());
            g2.drawOval(mx, my, markerR * 2, markerR * 2);

            legendDrawn.put(bar.getPattern(), true);
        }

        drawLegend(g2, W, legendDrawn);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        String title = ticker + "  —  Intraday Candlestick Chart with Patterns";
        g2.drawString(title, PAD_LEFT, 20);
    }

    private int priceToY(double price, double minP, double priceRange, int chartH) {
        return PAD_TOP + chartH - (int)(chartH * (price - minP) / priceRange);
    }

    private void drawLegend(Graphics2D g2, int W, Map<String, Boolean> present) {
        if (present.isEmpty()) return;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int lx = W - PAD_RIGHT - 160;
        int ly = PAD_TOP + 10;
        int lh = (present.size() + 2) * 18 + 10;
        g2.setColor(LABEL_BG);
        g2.fillRoundRect(lx - 8, ly - 14, 165, lh, 8, 8);
        g2.setColor(AXIS_LINE);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(lx - 8, ly - 14, 165, lh, 8, 8);

        int row = 0;
        drawLegendRow(g2, lx, ly + row * 18, BULL, "Bullish candle"); row++;
        drawLegendRow(g2, lx, ly + row * 18, BEAR, "Bearish candle"); row++;

        for (Map.Entry<String, Boolean> e : present.entrySet()) {
            Color pc = PATTERN_COLORS.getOrDefault(e.getKey(), Color.YELLOW);
            drawLegendDot(g2, lx, ly + row * 18, pc, e.getKey()); row++;
        }
    }

    private void drawLegendRow(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillRect(x, y - 8, 14, 10);
        g2.setColor(AXIS_TEXT);
        g2.drawString(label, x + 20, y);
    }

    private void drawLegendDot(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillOval(x + 2, y - 8, 10, 10);
        g2.setColor(AXIS_TEXT);
        g2.drawString(label, x + 20, y);
    }

    private void drawEmpty(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0x37474F));
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        String msg = "Enter a ticker and click Fetch";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }
}
