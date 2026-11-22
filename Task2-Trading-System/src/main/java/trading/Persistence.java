package trading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Persistence {
    private static final String ORDER_HEADER = "user,side,symbol,qty,price";
    private static final String TRADE_HEADER = "time,symbol,qty,price,buyer,seller";

    private final Path ordersFile;
    private final Path tradesFile;

    public Persistence(Path dir) {
        this.ordersFile = dir.resolve("orderbook.csv");
        this.tradesFile = dir.resolve("trades.csv");
    }

    public synchronized void saveOrders(List<Order> orders) throws IOException {
        StringBuilder sb = new StringBuilder(ORDER_HEADER).append(System.lineSeparator());
        for (Order o : orders) {
            sb.append(clean(o.user())).append(',')
              .append(o.side()).append(',')
              .append(clean(o.symbol())).append(',')
              .append(o.qty()).append(',')
              .append(o.price()).append(System.lineSeparator());
        }
        Path tmp = ordersFile.resolveSibling(ordersFile.getFileName() + ".tmp");
        Files.writeString(tmp, sb.toString());
        Files.move(tmp, ordersFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public synchronized List<Order> loadOrders() {
        List<Order> orders = new ArrayList<>();
        if (!Files.exists(ordersFile)) {
            return orders;
        }
        try {
            for (String line : Files.readAllLines(ordersFile)) {
                if (line.isBlank() || line.equals(ORDER_HEADER)) {
                    continue;
                }
                String[] p = line.split(",", -1);
                try {
                    if (p.length != 5) {
                        throw new NumberFormatException("wrong column count");
                    }
                    orders.add(new Order(p[0], p[1], p[2], Integer.parseInt(p[3]), Double.parseDouble(p[4])));
                } catch (NumberFormatException e) {
                    System.out.println("Skipping bad line in " + ordersFile + ": " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + ordersFile + ": " + e.getMessage());
        }
        return orders;
    }

    public synchronized void appendTrade(Trade t) throws IOException {
        if (!Files.exists(tradesFile)) {
            Files.writeString(tradesFile, TRADE_HEADER + System.lineSeparator());
        }
        String line = t.time() + "," + clean(t.symbol()) + "," + t.qty() + "," + t.price() + ","
            + clean(t.buyer()) + "," + clean(t.seller()) + System.lineSeparator();
        Files.writeString(tradesFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized List<Trade> loadTrades(int max) {
        List<Trade> trades = new ArrayList<>();
        if (!Files.exists(tradesFile)) {
            return trades;
        }
        try {
            for (String line : Files.readAllLines(tradesFile)) {
                if (line.isBlank() || line.equals(TRADE_HEADER)) {
                    continue;
                }
                String[] p = line.split(",", -1);
                try {
                    if (p.length != 6) {
                        throw new NumberFormatException("wrong column count");
                    }
                    trades.add(new Trade(p[1], Integer.parseInt(p[2]), Double.parseDouble(p[3]),
                        p[4], p[5], Long.parseLong(p[0])));
                } catch (NumberFormatException e) {
                    System.out.println("Skipping bad line in " + tradesFile + ": " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + tradesFile + ": " + e.getMessage());
        }
        int from = Math.max(0, trades.size() - max);
        return new ArrayList<>(trades.subList(from, trades.size()));
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace(',', '_').replace('\n', '_').replace('\r', '_');
    }
}
