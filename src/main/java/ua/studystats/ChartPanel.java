package ua.studystats;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Locale;

/** Lightweight custom plots, rendered using Java2D (no external dependencies). */
public final class ChartPanel extends JPanel {
    private enum Type { EMPTY, HISTOGRAM, SCATTER }
    private Type type = Type.EMPTY;
    private double[] x = new double[0];
    private double[] y = new double[0];
    private int bins = 10;
    private String xLabel = "";
    private String yLabel = "";
    private boolean showRegression = false;
    private Statistics.Regression regression;

    private static final Color INK = new Color(38, 49, 78);
    private static final Color MUTED = new Color(118, 130, 151);
    private static final Color GRID = new Color(232, 237, 246);
    private static final Color BLUE = new Color(63, 103, 213);
    private static final Color TEAL = new Color(29, 159, 155);
    private static final Color ORANGE = new Color(237, 126, 69);

    public ChartPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(660, 390));
    }

    public void showHistogram(double[] values, int bins, String label) {
        this.type = Type.HISTOGRAM;
        this.x = values.clone();
        this.y = new double[0];
        this.bins = Math.max(2, Math.min(60, bins));
        this.xLabel = label;
        this.yLabel = "Частота";
        repaint();
    }

    public void showScatter(double[] xx, double[] yy, String xLabel, String yLabel,
                            boolean showRegression, Statistics.Regression regression) {
        this.type = Type.SCATTER;
        this.x = xx.clone();
        this.y = yy.clone();
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.showRegression = showRegression;
        this.regression = regression;
        repaint();
    }

    public void clear() {
        type = Type.EMPTY;
        x = new double[0];
        y = new double[0];
        repaint();
    }

    @Override protected void paintComponent(Graphics original) {
        super.paintComponent(original);
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (type == Type.EMPTY || x.length == 0) {
                g.setColor(MUTED);
                g.setFont(getFont().deriveFont(Font.PLAIN, 15f));
                center(g, "Оберіть стовпець, щоб побудувати графік", getWidth() / 2, getHeight() / 2);
                return;
            }
            int left = 78, right = 28, top = 30, bottom = 76;
            int width = getWidth() - left - right;
            int height = getHeight() - top - bottom;
            if (width < 60 || height < 60) return;
            if (type == Type.HISTOGRAM) histogram(g, left, top, width, height);
            else scatter(g, left, top, width, height);
            g.setColor(INK);
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            center(g, xLabel, left + width / 2, top + height + 53);
            g.rotate(-Math.PI / 2);
            center(g, yLabel, -(top + height / 2), 22);
        } finally {
            g.dispose();
        }
    }

    private void histogram(Graphics2D g, int left, int top, int width, int height) {
        Range range = range(x);
        double min = range.min;
        double max = range.max;
        double delta = max - min;
        int[] counts = new int[bins];
        for (double value : x) {
            int index = (int) Math.floor((value - min) / delta * bins);
            counts[Math.max(0, Math.min(bins - 1, index))]++;
        }
        int maxCount = 1;
        for (int count : counts) maxCount = Math.max(maxCount, count);
        int topCount = Math.max(4, maxCount);
        axes(g, left, top, width, height, min, max, 0, topCount, true);
        double bandWidth = (double) width / bins;
        for (int i = 0; i < bins; i++) {
            double px = left + i * bandWidth;
            double barHeight = (double) counts[i] / topCount * height;
            g.setColor(i % 2 == 0 ? BLUE : TEAL);
            int bx = (int) Math.round(px + Math.min(2, bandWidth * .08));
            int bw = Math.max(1, (int) Math.round(bandWidth - Math.min(4, bandWidth * .16)));
            g.fillRoundRect(bx, (int) Math.round(top + height - barHeight), bw,
                    Math.max(0, (int) Math.round(barHeight)), 5, 5);
        }
    }

    private void scatter(Graphics2D g, int left, int top, int width, int height) {
        Range rx = padded(range(x));
        Range ry = padded(range(y));
        axes(g, left, top, width, height, rx.min, rx.max, ry.min, ry.max, false);
        g.setColor(new Color(63, 103, 213, 160));
        for (int i = 0; i < x.length; i++) {
            double px = left + (x[i] - rx.min) / (rx.max - rx.min) * width;
            double py = top + height - (y[i] - ry.min) / (ry.max - ry.min) * height;
            g.fill(new Ellipse2D.Double(px - 4.5, py - 4.5, 9, 9));
        }
        if (showRegression && regression != null) {
            double ya = regression.predict(rx.min);
            double yb = regression.predict(rx.max);
            double pa = top + height - (ya - ry.min) / (ry.max - ry.min) * height;
            double pb = top + height - (yb - ry.min) / (ry.max - ry.min) * height;
            Shape oldClip = g.getClip();
            g.clipRect(left, top, width, height);
            g.setColor(ORANGE);
            g.setStroke(new BasicStroke(2.8f));
            g.draw(new Line2D.Double(left, pa, left + width, pb));
            g.setClip(oldClip);
        }
    }

    private void axes(Graphics2D g, int left, int top, int width, int height,
                      double xmin, double xmax, double ymin, double ymax, boolean integers) {
        g.setFont(getFont().deriveFont(11f));
        for (int tick = 0; tick <= 5; tick++) {
            int gx = left + (int) Math.round(width * tick / 5.0);
            int gy = top + height - (int) Math.round(height * tick / 5.0);
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(left, gy, left + width, gy);
            g.drawLine(gx, top, gx, top + height);
            g.setColor(MUTED);
            double xv = xmin + (xmax - xmin) * tick / 5.0;
            double yv = ymin + (ymax - ymin) * tick / 5.0;
            String xs = format(xv);
            String ys = integers ? String.format(Locale.US, "%.0f", yv) : format(yv);
            center(g, xs, gx, top + height + 22);
            g.drawString(ys, left - 12 - g.getFontMetrics().stringWidth(ys), gy + 4);
        }
        g.setColor(new Color(179, 190, 207));
        g.drawLine(left, top, left, top + height);
        g.drawLine(left, top + height, left + width, top + height);
    }

    private static String format(double number) {
        if (Math.abs(number) >= 1000 || Math.abs(number) < .001 && number != 0) {
            return String.format(Locale.US, "%.1e", number);
        }
        return String.format(Locale.US, "%.2f", number);
    }

    private static void center(Graphics2D g, String text, int centerX, int baselineY) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baselineY);
    }

    private record Range(double min, double max) {}
    private static Range range(double[] values) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            min = Math.min(value, min);
            max = Math.max(value, max);
        }
        if (max == min) {
            double pad = Math.max(1, Math.abs(max) * .05);
            return new Range(min - pad, max + pad);
        }
        return new Range(min, max);
    }
    private static Range padded(Range raw) {
        double delta = raw.max - raw.min;
        double pad = delta * .08;
        return new Range(raw.min - pad, raw.max + pad);
    }
}
