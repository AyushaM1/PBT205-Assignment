package trading;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

public class LineChart extends JPanel {
    private static final int MAX_POINTS = 100;
    private static final Color[] COLORS = {
        new Color(0xFF6600), new Color(0x1F77B4), new Color(0x2CA02C), new Color(0x9467BD)};

    private final Map<String, List<Double>> series = new LinkedHashMap<>();

    public LineChart() {
        setBackground(Color.WHITE);
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();

        int w = getWidth();
        int h = getHeight();
        int left = 60;
        int right = 20;
        int top = 36;
        int bottom = 28;
        int gap = 22;
        int plotW = w - left - right;

        g2.setColor(Color.DARK_GRAY);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString("Trade prices", left, 22);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));

        int count = series.size();
        if (count == 0 || plotW <= 0) {
            g2.setColor(Color.GRAY);
            g2.drawString("No trades yet", left + 10, top + 30);
            g2.dispose();
            return;
        }

        int panelH = (h - top - bottom - gap * (count - 1)) / count;
        if (panelH <= 20) {
            g2.dispose();
            return;
        }

        int idx = 0;
        for (Map.Entry<String, List<Double>> e : series.entrySet()) {
            Color c = COLORS[idx % COLORS.length];
            List<Double> pts = e.getValue();
            int y0 = top + idx * (panelH + gap);

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (double v : pts) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            if (max - min < 0.5) {
                double mid = (max + min) / 2;
                min = mid - 0.25;
                max = mid + 0.25;
            }
            double pad = (max - min) * 0.1;
            min -= pad;
            max += pad;

            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i <= 2; i++) {
                int y = y0 + panelH - (int) Math.round(panelH * i / 2.0);
                g2.setColor(new Color(0xE5E5E5));
                g2.drawLine(left, y, left + plotW, y);
                g2.setColor(Color.GRAY);
                String label = String.format("%.2f", min + (max - min) * i / 2.0);
                g2.drawString(label, left - 8 - fm.stringWidth(label), y + 4);
            }
            g2.setColor(Color.GRAY);
            g2.drawRect(left, y0, plotW, panelH);

            g2.setColor(c);
            g2.fillRect(left + 8, y0 + 8, 10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(e.getKey(), left + 24, y0 + 18);

            g2.setColor(c);
            g2.setStroke(new BasicStroke(2f));
            int n = pts.size();
            int prevX = 0;
            int prevY = 0;
            for (int i = 0; i < n; i++) {
                int x = n == 1 ? left + plotW / 2 : left + (int) Math.round(plotW * i / (double) (n - 1));
                int y = y0 + panelH - (int) Math.round((pts.get(i) - min) / (max - min) * panelH);
                if (i > 0) {
                    g2.drawLine(prevX, prevY, x, y);
                }
                g2.fillOval(x - 3, y - 3, 6, 6);
                prevX = x;
                prevY = y;
            }
            idx++;
        }

        g2.setColor(Color.GRAY);
        g2.drawString("older", left, h - 8);
        String newest = "newest";
        g2.drawString(newest, left + plotW - fm.stringWidth(newest), h - 8);
        g2.dispose();
    }
}
