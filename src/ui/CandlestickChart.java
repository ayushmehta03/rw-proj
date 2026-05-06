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

    private static final Color BG          = new Color(0x0B0E14);
    private static final Color GRID        = new Color(0x1E222D);
    private static final Color BULL        = new Color(0x26A69A);   
    private static final Color BEAR        = new Color(0xEF5350);   
    private static final Color AXIS_TEXT   = new Color(0x707A8A);
    private static final Color AXIS_LINE   = new Color(0x2B3139);
    private static final Color HIGHLIGHT   = new Color(0x2962FF);
    private static final Color LABEL_BG    = new Color(18, 22, 30, 240);

    private static final Map<String, Color> PATTERN_COLORS = new HashMap<>();
    static {
        PATTERN_COLORS.put("Doji",      new Color(0x90A4AE));
        PATTERN_COLORS.put("Hammer",    new Color(0xFFD600));
        PATTERN_COLORS.put("Engulfing", new Color(0xBB86FC));
    }

    private static final int PAD_LEFT   = 65;
    private static final int PAD_RIGHT  = 15;
    private static final int PAD_TOP    = 50;
    private static final int PAD_BOTTOM = 55;

    private List<StockData> data;
    private String ticker = "";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM");

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
        double pad  = (maxP - minP) * 0.12;
        minP -= pad;  maxP += pad;
        double priceRange = maxP - minP;

        int n = data.size();
        double slotW = (double) chartW / n;
        double candleW = Math.max(3, slotW * 0.75);

        g2.setFont(new Font("Inter", Font.PLAIN, 11));
        int yTicks = 8;
        for (int i = 0; i <= yTicks; i++) {
            double price = minP + priceRange * i / yTicks;
            int y = PAD_TOP + chartH - (int)(chartH * (price - minP) / priceRange);

            g2.setColor(GRID);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);

            g2.setColor(AXIS_TEXT);
            String label = String.format("%.2f", price);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, PAD_LEFT - fm.stringWidth(label) - 10, y + 4);
        }

        for (int i = 0; i < n; i++) {
            StockData bar = data.get(i);
            double xCenter = PAD_LEFT + i * slotW + slotW / 2.0;

            int yHigh  = priceToY(bar.getHigh(),  minP, priceRange, chartH);
            int yLow   = priceToY(bar.getLow(),   minP, priceRange, chartH);
            int yOpen  = priceToY(bar.getOpen(),  minP, priceRange, chartH);
            int yClose = priceToY(bar.getClose(), minP, priceRange, chartH);

            Color c = bar.isBullish() ? BULL : BEAR;
            g2.setColor(c);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine((int) xCenter, yHigh, (int) xCenter, yLow);

            int bodyTop = Math.min(yOpen, yClose);
            int bodyH   = Math.max(2, Math.abs(yClose - yOpen));
            int bodyX   = (int)(xCenter - candleW / 2);

            GradientPaint gp = new GradientPaint(bodyX, bodyTop, c, bodyX + (int)candleW, bodyTop + bodyH, c.darker());
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Double(bodyX, bodyTop, candleW, bodyH, 2, 2));
            
            g2.setColor(c.brighter());
            g2.setStroke(new BasicStroke(0.5f));
            g2.draw(new RoundRectangle2D.Double(bodyX, bodyTop, candleW, bodyH, 2, 2));
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(AXIS_TEXT);
        int labelEvery = Math.max(1, n / 8);
        for (int i = 0; i < n; i += labelEvery) {
            double xCenter = PAD_LEFT + i * slotW + slotW / 2.0;
            String ts = data.get(i).getDateTime().format(data.size() > 60 ? TIME_FMT : DT_FMT);
            g2.drawString(ts, (int) xCenter - 15, PAD_TOP + chartH + 20);
        }

        Map<String, Boolean> legendDrawn = new HashMap<>();
        for (int i = 0; i < n; i++) {
            StockData bar = data.get(i);
            if (bar.getPattern() == null) continue;

            double xCenter = PAD_LEFT + i * slotW + slotW / 2.0;
            int yCoord = bar.isBullish() ? priceToY(bar.getLow(), minP, priceRange, chartH) + 15 : 
                                         priceToY(bar.getHigh(), minP, priceRange, chartH) - 15;

            Color pc = PATTERN_COLORS.getOrDefault(bar.getPattern(), Color.WHITE);
            g2.setColor(pc);
            g2.fillOval((int)xCenter - 4, yCoord - 4, 8, 8);
            legendDrawn.put(bar.getPattern(), true);
        }

        drawHeader(g2, W);
        drawLegend(g2, W, legendDrawn);
    }

    private void drawHeader(Graphics2D g2, int W) {
        g2.setFont(new Font("Inter", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        g2.drawString(ticker.toUpperCase(), PAD_LEFT, 30);
        
        g2.setFont(new Font("Inter", Font.PLAIN, 12));
        g2.setColor(AXIS_TEXT);
        g2.drawString("• Real-time Market Data", PAD_LEFT + 100, 28);
        
        g2.setColor(AXIS_LINE);
        g2.drawLine(0, 45, W, 45);
    }

    private int priceToY(double price, double minP, double priceRange, int chartH) {
        return PAD_TOP + chartH - (int)(chartH * (price - minP) / priceRange);
    }

    private void drawLegend(Graphics2D g2, int W, Map<String, Boolean> present) {
        g2.setFont(new Font("Inter", Font.PLAIN, 11));
        int lx = W - 180;
        int ly = 65;
        int lh = (present.size() + 2) * 20 + 10;

        g2.setColor(LABEL_BG);
        g2.fillRoundRect(lx, ly, 160, lh, 10, 10);
        g2.setColor(AXIS_LINE);
        g2.drawRoundRect(lx, ly, 160, lh, 10, 10);

        int row = 0;
        drawLegendRow(g2, lx + 10, ly + 20 + row * 20, BULL, "Bullish"); row++;
        drawLegendRow(g2, lx + 10, ly + 20 + row * 20, BEAR, "Bearish"); row++;

        for (String pattern : present.keySet()) {
            drawLegendDot(g2, lx + 10, ly + 20 + row * 20, PATTERN_COLORS.get(pattern), pattern); row++;
        }
    }

    private void drawLegendRow(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillRoundRect(x, y - 9, 12, 12, 3, 3);
        g2.setColor(Color.WHITE);
        g2.drawString(label, x + 20, y);
    }

    private void drawLegendDot(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillOval(x + 2, y - 8, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString(label, x + 20, y);
    }
private void drawEmpty(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AXIS_TEXT);
        // Changed Font.MEDIUM to Font.PLAIN
        g2.setFont(new Font("Inter", Font.PLAIN, 16)); 
        String msg = "Awaiting Market Connection...";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }
}