package service;

import model.StockData;
import java.util.List;

/**
 * Pure-Java candlestick pattern detector.
 * Mirrors TA-Lib's CDL_DOJI, CDL_HAMMER, CDL_ENGULFING logic.
 */
public class PatternDetector {

    public static void detect(List<StockData> data) {
        for (int i = 0; i < data.size(); i++) {
            StockData bar = data.get(i);

            String pattern = null;

            if (isDoji(bar)) {
                pattern = "Doji";
            } else if (isHammer(bar)) {
                pattern = "Hammer";
            } else if (i > 0 && isEngulfing(data.get(i - 1), bar)) {
                pattern = "Engulfing";
            }

            bar.setPattern(pattern);
        }
    }

    // ── Doji: body ≤ 10 % of total range ────────────────────────────────────
    private static boolean isDoji(StockData b) {
        double range = b.getHigh() - b.getLow();
        if (range == 0) return false;
        double body = Math.abs(b.getClose() - b.getOpen());
        return body / range <= 0.10;
    }

    // ── Hammer: small body at top, long lower shadow (≥ 2× body), tiny upper shadow
    private static boolean isHammer(StockData b) {
        double body       = Math.abs(b.getClose() - b.getOpen());
        double upperShadow = b.getHigh() - Math.max(b.getOpen(), b.getClose());
        double lowerShadow = Math.min(b.getOpen(), b.getClose()) - b.getLow();
        double range       = b.getHigh() - b.getLow();
        if (range == 0 || body == 0) return false;
        return lowerShadow >= 2.0 * body
            && upperShadow <= 0.1 * range;
    }

    // ── Engulfing: current body fully engulfs previous body, opposite colours
    private static boolean isEngulfing(StockData prev, StockData curr) {
        boolean prevBear = prev.getClose() < prev.getOpen();
        boolean currBull = curr.getClose() > curr.getOpen();
        boolean prevBull = prev.getClose() > prev.getOpen();
        boolean currBear = curr.getClose() < curr.getOpen();

        double prevBodyHi = Math.max(prev.getOpen(), prev.getClose());
        double prevBodyLo = Math.min(prev.getOpen(), prev.getClose());
        double currBodyHi = Math.max(curr.getOpen(), curr.getClose());
        double currBodyLo = Math.min(curr.getOpen(), curr.getClose());

        boolean bullEngulf = prevBear && currBull
            && currBodyHi > prevBodyHi && currBodyLo < prevBodyLo;
        boolean bearEngulf = prevBull && currBear
            && currBodyHi > prevBodyHi && currBodyLo < prevBodyLo;

        return bullEngulf || bearEngulf;
    }
}
