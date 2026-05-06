package model;

import java.time.LocalDateTime;

public class StockData {
    private final LocalDateTime dateTime;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;
    private String pattern; // null = no pattern

    public StockData(LocalDateTime dateTime, double open, double high, double low, double close, long volume) {
        this.dateTime = dateTime;
        this.open     = open;
        this.high     = high;
        this.low      = low;
        this.close    = close;
        this.volume   = volume;
        this.pattern  = null;
    }

    public LocalDateTime getDateTime() { return dateTime; }
    public double getOpen()  { return open;  }
    public double getHigh()  { return high;  }
    public double getLow()   { return low;   }
    public double getClose() { return close; }
    public long   getVolume(){ return volume; }
    public String getPattern()              { return pattern; }
    public void   setPattern(String pattern){ this.pattern = pattern; }

    public boolean isBullish() { return close > open; }
}
