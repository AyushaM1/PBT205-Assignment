package trading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves the open order book to orderbook.csv so the Exchange can recover after a
 * restart (FR1). Trades are saved separately by TradeHistory (FR2).
 * Columns: user,side,symbol,qty,price
 */
public class Persistence {
    private static final String HEADER = "user,side,symbol,qty,price";

    private final Path ordersFile;

    public Persistence(Path dir) {
        this.ordersFile = dir.resolve("orderbook.csv");
    }

    public synchronized void saveOrders(List<Order> orders) throws IOException {
        StringBuilder sb = new StringBuilder(HEADER).append(System.lineSeparator());
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
                if (line.isBlank() || line.equals(HEADER)) {
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

    private static String clean(String s) {
        return s == null ? "" : s.replace(',', '_').replace('\n', '_').replace('\r', '_');
    }
}
