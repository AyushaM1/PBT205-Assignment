
package trading;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

public class LineChart extends JPanel {

    private static final int MAX_POINTS = 100;

    private static final Color BACKGROUND = new Color(16, 24, 39);
    private static final Color PANEL = new Color(30, 41, 59);
    private static final Color GRID = new Color(51, 65, 85);
    private static final Color TEXT = new Color(148, 163, 184);
    private static final Color WHITE = new Color(248, 250, 252);

    private static final Color[] COLORS = {
            new Color(16, 185, 129),
            new Color(56, 189, 248),
            new Color(250, 204, 21),
            new Color(168, 85, 247)
    };

    private final Map<String, List<Double>> series = new LinkedHashMap<>();

    public LineChart() {
        setBackground(BACKGROUND);
        setOpaque(true);
    }

    public synchronized void addPoint(String name, double value) {
        List<Double> points = series.computeIfAbsent(name, k -> new ArrayList<>());

        points.add(value);

        if (points.size() > MAX_POINTS) {
            points.remove(0);
        }

        repaint();
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.drawString("PRICE HISTORY", 30, 35);

            g2.setColor(TEXT);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(
                    "Historical executed trade prices", 30, 55);

            if (series.isEmpty()) {
                g2.setColor(TEXT);
                g2.drawString(
                        "Waiting for trading activity...",
                        40, Math.max(100, height / 2));
                return;
            }

            int left = 85;
            int right = 65;
            int top = 95;
            int bottom = 40;
            int gap = 35;

            int plotWidth = width - left - right;
            int count = series.size();
            int panelHeight = (height - top - bottom - gap * (count - 1))
                    / count;

            if (plotWidth <= 20 || panelHeight <= 30) {
                return;
            }

            int index = 0;

            for (Map.Entry<String, List<Double>> entry : series.entrySet()) {

                String symbol = entry.getKey();
                List<Double> points = entry.getValue();

                if (points.isEmpty()) {
                    continue;
                }

                Color accent = COLORS[index % COLORS.length];
                int yTop = top + index * (panelHeight + gap);

                g2.setColor(PANEL);
                g2.fillRoundRect(
                        left - 15,
                        yTop - 25,
                        plotWidth + 40,
                        panelHeight + 40,
                        14,
                        14);

                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;

                for (double value : points) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }

                if (max - min < 0.5) {
                    double mid = (min + max) / 2.0;
                    min = mid - 0.25;
                    max = mid + 0.25;
                }

                double padding = (max - min) * 0.1;
                min -= padding;
                max += padding;

                g2.setFont(
                        new Font("SansSerif", Font.BOLD, 13));
                g2.setColor(accent);
                g2.drawString(symbol, left, yTop - 7);

                double latest = points.get(points.size() - 1);

                String latestText = String.format("$%.2f", latest);

                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(
                        latestText,
                        left + plotWidth
                                - metrics.stringWidth(latestText),
                        yTop - 7);

                g2.setFont(
                        new Font("SansSerif", Font.PLAIN, 11));

                for (int i = 0; i <= 4; i++) {
                    int y = yTop + panelHeight
                            - (int) Math.round(
                                    panelHeight * i / 4.0);

                    g2.setColor(GRID);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine(
                            left, y,
                            left + plotWidth, y);

                    String label = String.format(
                            "%.2f",
                            min + (max - min) * i / 4.0);

                    g2.setColor(TEXT);
                    g2.drawString(
                            label,
                            left - 12
                                    - g2.getFontMetrics()
                                            .stringWidth(label),
                            y + 4);
                }

                Path2D line = new Path2D.Double();
                Path2D area = new Path2D.Double();

                int n = points.size();
                int lastX = left;
                int lastY = yTop + panelHeight;

                for (int i = 0; i < n; i++) {
                    int x = n == 1
                            ? left + plotWidth / 2
                            : left + (int) Math.round(
                                    plotWidth * i / (double) (n - 1));

                    int y = yTop + panelHeight
                            - (int) Math.round(
                                    (points.get(i) - min)
                                            / (max - min) * panelHeight);

                    if (i == 0) {
                        line.moveTo(x, y);
                        area.moveTo(x, yTop + panelHeight);
                        area.lineTo(x, y);
                    } else {
                        line.lineTo(x, y);
                        area.lineTo(x, y);
                    }

                    lastX = x;
                    lastY = y;
                }

                area.lineTo(lastX, yTop + panelHeight);
                area.closePath();

                g2.setColor(new Color(
                        accent.getRed(),
                        accent.getGreen(),
                        accent.getBlue(),
                        35));
                g2.fill(area);

                g2.setColor(accent);
                g2.setStroke(
                        new BasicStroke(
                                2.5f,
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND));
                g2.draw(line);

                // Draw markers for every executed trade
                for (int i = 0; i < n; i++) {
                    int x = n == 1
                            ? left + plotWidth / 2
                            : left + (int) Math.round(
                                    plotWidth * i / (double) (n - 1));

                    int y = yTop + panelHeight
                            - (int) Math.round(
                                    (points.get(i) - min)
                                            / (max - min) * panelHeight);

                    g2.setColor(accent);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                }

                g2.setColor(TEXT);
                g2.setFont(
                        new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(
                        "Older trades",
                        left,
                        yTop + panelHeight + 23);

                String newest = "Latest trades";
                g2.drawString(
                        newest,
                        left + plotWidth
                                - g2.getFontMetrics()
                                        .stringWidth(newest),
                        yTop + panelHeight + 23);

                index++;
            }

        } finally {
            g2.dispose();
        }
    }
}
