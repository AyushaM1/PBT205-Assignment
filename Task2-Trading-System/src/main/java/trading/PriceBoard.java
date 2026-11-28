package trading;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class PriceBoard {
    private static final Map<String, String> NAMES = Map.of(
        "XYZ", "XYZ Corp",
        "TORR", "Torrens University Australia");

    private static final int MAX_HISTORY = 200;
    private static final Path CLOSE_FILE = Path.of("closing_prices.csv");

    private static Map<String, String> loadPreviousCloses() {
        Map<String, String> closes = new LinkedHashMap<>();
        if (!Files.exists(CLOSE_FILE)) {
            return closes;
        }
        try {
            for (String line : Files.readAllLines(CLOSE_FILE)) {
                String[] parts = line.split(",");
                if (parts.length == 3 && !parts[0].equals("session_end")) {
                    closes.put(parts[1], parts[2]);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + CLOSE_FILE + ": " + e.getMessage());
        }
        return closes;
    }

    private static void saveClosingPrices(Map<String, Double> lastPrice) {
        if (lastPrice.isEmpty()) {
            return;
        }
        try {
            if (!Files.exists(CLOSE_FILE)) {
                Files.writeString(CLOSE_FILE, "session_end,symbol,closing_price" + System.lineSeparator());
            }
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> e : lastPrice.entrySet()) {
                sb.append(now).append(",").append(e.getKey()).append(",").append(e.getValue())
                  .append(System.lineSeparator());
            }
            Files.writeString(CLOSE_FILE, sb.toString(), StandardOpenOption.APPEND);
            System.out.println("Saved closing prices to " + CLOSE_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Could not save closing prices: " + e.getMessage());
        }
    }

    private static JLabel createStockCard(
        String title, String value) {

        JLabel label = new JLabel(
            "<html><span style='font-size:11px;color:#94A3B8;'>"
            + title
            + "</span><br><br>"
            + "<span style='font-size:22px;color:#F8FAFC;'>"
            + value
            + "</span></html>"
        );

        label.setOpaque(true);
        label.setBackground(new Color(30, 41, 59));
        label.setBorder(new EmptyBorder(18, 20, 18, 20));

    return label;
    }

    private static void refreshDashboardCards(
            JLabel xyzCard,
            JLabel torrCard,
            JLabel tradesCard,
            DefaultTableModel latestModel,
            DefaultTableModel historyModel) {

        for (int i = 0; i < latestModel.getRowCount(); i++) {
            String symbol = latestModel.getValueAt(i, 0).toString();
            Object rawPrice = latestModel.getValueAt(i, 2);
            String price;

            if (rawPrice instanceof Number) {
                price = String.format("$%.2f", ((Number) rawPrice).doubleValue());
            } else {
                try {
                    price = String.format("$%.2f",
                            Double.parseDouble(rawPrice.toString()));
                } catch (NumberFormatException ex) {
                    price = "—";
                }
            }

            if (symbol.equals("XYZ")) {
                xyzCard.setText(cardHtml("XYZ / LAST PRICE", price));
            } else if (symbol.equals("TORR")) {
                torrCard.setText(cardHtml("TORR / LAST PRICE", price));
            }
        }

        tradesCard.setText(cardHtml(
            "TOTAL TRADES",
            String.valueOf(historyModel.getRowCount())
        ));
    }

    private static String cardHtml(String title, String value) {
        return "<html><span style='font-size:11px;color:#94A3B8;'>"
            + title
            + "</span><br><br>"
            + "<span style='font-size:22px;color:#F8FAFC;'>"
            + value
            + "</span></html>";
    }

    public static void main(String[] args) throws Exception {
        FlatDarkLaf.setup();
        UIManager.put("Panel.background", new Color(16, 24, 39));
        UIManager.put("Table.background", new Color(30, 41, 59));
        UIManager.put("Table.foreground", new Color(248, 250, 252));
        UIManager.put("Table.selectionBackground", new Color(51, 65, 85));
        UIManager.put("Table.gridColor", new Color(51, 65, 85));
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("TableHeader.background", new Color(30, 41, 59));
        UIManager.put("TableHeader.foreground", new Color(148, 163, 184));
        UIManager.put("TabbedPane.background", new Color(16, 24, 39));
        UIManager.put("TabbedPane.foreground", new Color(248, 250, 252));
        UIManager.put("ScrollPane.background", new Color(30, 41, 59));
        int port = Integer.parseInt(args[0]);

        Map<String, String> previousCloses = loadPreviousCloses();
        Map<String, Double> lastPrice = new ConcurrentHashMap<>();

        DefaultTableModel latestModel = new DefaultTableModel(
            new Object[] {"Symbol", "Company", "Latest price", "Previous close"}, 0);
        DefaultTableModel historyModel = new DefaultTableModel(
            new Object[] {"Time", "Symbol", "Price", "Qty", "Buyer", "Seller"}, 0);
        Map<String, Integer> rowOf = new HashMap<>();

        Set<String> symbols = new LinkedHashSet<>(java.util.List.of("XYZ", "TORR"));
        symbols.addAll(previousCloses.keySet());
        for (String symbol : symbols) {
            latestModel.addRow(new Object[] {
                symbol, NAMES.getOrDefault(symbol, symbol), "no trades yet",
                previousCloses.getOrDefault(symbol, "-")});
            rowOf.put(symbol, latestModel.getRowCount() - 1);
        }

        LineChart chart = new LineChart();
        DepthPanel depth = new DepthPanel(new ArrayList<>(symbols));
        DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

        SwingUtilities.invokeAndWait(() -> {
            JScrollPane latestPane = new JScrollPane(new JTable(latestModel));
            latestPane.setBorder(BorderFactory.createTitledBorder("Latest price"));
            JScrollPane historyPane = new JScrollPane(new JTable(historyModel));
            historyPane.setBorder(BorderFactory.createTitledBorder("Trade history (newest first)"));

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, latestPane, historyPane);
            split.setResizeWeight(0.3);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Prices", split);
            tabs.addTab("Chart", chart);
            tabs.addTab("Order book", depth);

            
            JFrame frame = new JFrame("PBT205 | Trading Terminal");

            JPanel dashboard = new JPanel(new BorderLayout(0, 18));
            dashboard.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel heading = new JLabel("TRADING TERMINAL");
            heading.setFont(new Font("SansSerif", Font.BOLD, 24));
            heading.setForeground(new Color(248, 250, 252));

            JLabel subtitle = new JLabel(
                "Market Overview  |  PBT205 Trading System"
            );
            subtitle.setForeground(new Color(148, 163, 184));

            JPanel headerText = new JPanel();
            headerText.setLayout(
                new BoxLayout(headerText, BoxLayout.Y_AXIS)
            );
            headerText.add(heading);
            headerText.add(Box.createVerticalStrut(5));
            headerText.add(subtitle);

            JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
            JLabel xyzCard = createStockCard(
                "XYZ / LAST PRICE", "Awaiting trade"
            );
            JLabel torrCard = createStockCard(
                "TORR / LAST PRICE", "Awaiting trade"
            );
            JLabel tradesCard = createStockCard(
                "TOTAL TRADES", "0"
            );

            cards.add(xyzCard);
            cards.add(torrCard);
            cards.add(tradesCard);


            Runnable refreshCards = () -> refreshDashboardCards(
                xyzCard,
                torrCard,
                tradesCard,
                latestModel,
                historyModel
        );

latestModel.addTableModelListener(e -> refreshCards.run());
historyModel.addTableModelListener(e -> refreshCards.run());

refreshCards.run();


            JPanel topSection = new JPanel(new BorderLayout(0, 16));
            topSection.add(headerText, BorderLayout.NORTH);
            topSection.add(cards, BorderLayout.CENTER);

            dashboard.add(topSection, BorderLayout.NORTH);
            dashboard.add(tabs, BorderLayout.CENTER);

            frame.setContentPane(dashboard);

            frame.setSize(1100, 750);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    saveClosingPrices(lastPrice);
                }
            });
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });

        Persistence store = new Persistence(Path.of("."));
        SwingUtilities.invokeLater(() -> {
            for (Trade t : store.loadTrades(MAX_HISTORY)) {
                showTrade(t, clock, latestModel, historyModel, rowOf, chart);
            }
        });

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(port);

        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        channel.exchangeDeclare("trades", BuiltinExchangeType.FANOUT, true);
        channel.exchangeDeclare("book", BuiltinExchangeType.FANOUT, true);
        String tradeQueue = channel.queueDeclare().getQueue();
        channel.queueBind(tradeQueue, "trades", "");
        String bookQueue = channel.queueDeclare().getQueue();
        channel.queueBind(bookQueue, "book", "");

        Gson gson = new Gson();

        DeliverCallback onTrade = (tag, delivery) -> {
            Trade trade = gson.fromJson(new String(delivery.getBody()), Trade.class);
            lastPrice.put(trade.symbol(), trade.price());
            SwingUtilities.invokeLater(() ->
                showTrade(trade, clock, latestModel, historyModel, rowOf, chart));
        };

        DeliverCallback onBook = (tag, delivery) -> {
            BookSnapshot snap = gson.fromJson(new String(delivery.getBody()), BookSnapshot.class);
            SwingUtilities.invokeLater(() -> depth.update(snap));
        };

        channel.basicConsume(tradeQueue, true, onTrade, tag -> {});
        channel.basicConsume(bookQueue, true, onBook, tag -> {});
    }

    private static void showTrade(Trade trade, DateTimeFormatter clock, DefaultTableModel latestModel,
                                  DefaultTableModel historyModel, Map<String, Integer> rowOf, LineChart chart) {
        String time = clock.format(Instant.ofEpochMilli(trade.time()));
        Integer row = rowOf.get(trade.symbol());
        if (row == null) {
            latestModel.addRow(new Object[] {
                trade.symbol(), NAMES.getOrDefault(trade.symbol(), trade.symbol()), trade.price(), "-"});
            rowOf.put(trade.symbol(), latestModel.getRowCount() - 1);
        } else {
            latestModel.setValueAt(trade.price(), row, 2);
        }

        historyModel.insertRow(0, new Object[] {
            time, trade.symbol(), trade.price(), trade.qty(), trade.buyer(), trade.seller()});
        if (historyModel.getRowCount() > MAX_HISTORY) {
            historyModel.removeRow(historyModel.getRowCount() - 1);
        }
        chart.addPoint(trade.symbol(), trade.price());
    }
}
