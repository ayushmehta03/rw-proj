package service;

import model.StockData;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.util.*;

public class YahooFinanceFetcher {

    public static List<StockData> fetch(String ticker, String interval, String period)
            throws Exception {

        String urlStr = String.format(
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=%s&range=%s",
            ticker.trim().toUpperCase(), interval, period
        );

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        int status = conn.getResponseCode();
        if (status != 200) {
            String errBody = "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder esb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) esb.append(line);
                errBody = esb.toString();
            } catch (Exception ignored) {}
            throw new Exception("HTTP " + status + " — Check ticker symbol.\n" + errBody);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        return parse(sb.toString());
    }


    private static List<StockData> parse(String json) throws Exception {
        String result = extractFirstResult(json);
        if (result == null)
            throw new Exception("Unexpected Yahoo Finance response format.");

        long[] timestamps = extractLongArray(result, "timestamp");
        if (timestamps == null || timestamps.length == 0)
            throw new Exception("No data returned. Market may be closed or ticker invalid.");

        String indicators = extractBlock(result, "indicators");
        String quoteArr   = extractBlock(indicators, "quote");
        String quote      = extractFirstObject(quoteArr);

        double[] opens  = extractDoubleArray(quote, "open");
        double[] highs  = extractDoubleArray(quote, "high");
        double[] lows   = extractDoubleArray(quote, "low");
        double[] closes = extractDoubleArray(quote, "close");
        long[]   vols   = extractLongArray(quote, "volume");

        int n = timestamps.length;
        List<StockData> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i >= opens.length || i >= highs.length ||
                i >= lows.length  || i >= closes.length) break;

            double o = opens[i], h = highs[i], l = lows[i], c = closes[i];
            if (Double.isNaN(o) || Double.isNaN(h) || Double.isNaN(l) || Double.isNaN(c)) continue;

            long epoch = timestamps[i];
            LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epoch), ZoneId.systemDefault());

            long vol = (vols != null && i < vols.length) ? vols[i] : 0L;
            list.add(new StockData(dt, o, h, l, c, vol));
        }
        return list;
    }

    private static String extractFirstResult(String json) {
        int idx = json.indexOf("\"result\"");
        if (idx < 0) return null;
        int bracket = json.indexOf('[', idx);
        if (bracket < 0) return null;
        return extractFirstObject(json.substring(bracket));
    }

    private static String extractFirstObject(String src) {
        if (src == null) return null;
        int start = src.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < src.length(); i++) {
            char ch = src.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') {
                depth--;
                if (depth == 0) return src.substring(start, i + 1);
            }
        }
        return null;
    }

    private static String extractBlock(String json, String key) {
        if (json == null) return null;
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + marker.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        char first = json.charAt(start);
        if (first == '{') return extractFirstObject(json.substring(start));
        if (first == '[') return extractBracketedBlock(json, start, '[', ']');
        return null;
    }

    private static String extractBracketedBlock(String json, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == open)  depth++;
            else if (ch == close) {
                depth--;
                if (depth == 0) return json.substring(start, i + 1);
            }
        }
        return null;
    }

    private static String extractArrayBlock(String json, String key) {
        if (json == null) return null;
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + marker.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (json.charAt(start) != '[') return null;
        return extractBracketedBlock(json, start, '[', ']');
    }

    private static double[] extractDoubleArray(String json, String key) {
        String block = extractArrayBlock(json, key);
        if (block == null) return new double[0];
        String inner = block.substring(1, block.length() - 1).trim();
        if (inner.isEmpty()) return new double[0];
        String[] tokens = inner.split(",");
        double[] arr = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i].trim();
            arr[i] = (t.equals("null") || t.isEmpty()) ? Double.NaN : Double.parseDouble(t);
        }
        return arr;
    }

    private static long[] extractLongArray(String json, String key) {
        String block = extractArrayBlock(json, key);
        if (block == null) return new long[0];
        String inner = block.substring(1, block.length() - 1).trim();
        if (inner.isEmpty()) return new long[0];
        String[] tokens = inner.split(",");
        long[] arr = new long[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i].trim();
            arr[i] = (t.equals("null") || t.isEmpty()) ? 0L : (long) Double.parseDouble(t);
        }
        return arr;
    }
}
