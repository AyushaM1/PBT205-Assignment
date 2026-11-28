package trading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Trading history (FR2). Every trade is appended to a CSV file, and the file is
 * read back when the program starts, so history survives an Exchange restart.
 * Columns: time,symbol,qty,price,buyer,seller
 */
public class TradeHistory {
    private static final String HEADER = "time,symbol,qty,price,buyer,seller";

    private final Path file;
    private final List<Trade> trades = new ArrayList<>();

    public TradeHistory(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.startsWith("time,")) {
                    continue;
                }
                try {
                    String[] p = line.split(",");
                    trades.add(new Trade(p[1], Integer.parseInt(p[2]), Double.parseDouble(p[3]),
                        p[4], p[5], Long.parseLong(p[0])));
                } catch (RuntimeException badLine) {
                    System.out.println("History: skipped bad line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read trade history: " + e.getMessage());
        }
    }

    /** Remember a trade in memory and append it to the file. */
    public synchronized void record(Trade t) {
        trades.add(t);
        try {
            boolean isNew = !Files.exists(file);
            StringBuilder sb = new StringBuilder();
            if (isNew) {
                sb.append(HEADER).append(System.lineSeparator());
            }
            sb.append(t.time()).append(',').append(t.symbol()).append(',').append(t.qty())
              .append(',').append(t.price()).append(',').append(clean(t.buyer()))
              .append(',').append(clean(t.seller())).append(System.lineSeparator());
            Files.writeString(file, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Could not save trade: " + e.getMessage());
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace(',', ' ');
    }

    public synchronized List<Trade> all() {
        return Collections.unmodifiableList(new ArrayList<>(trades));
    }

    public synchronized List<Trade> forSymbol(String symbol) {
        List<Trade> out = new ArrayList<>();
        for (Trade t : trades) {
            if (t.symbol().equalsIgnoreCase(symbol)) {
                out.add(t);
            }
        }
        return out;
    }

    /** Price of the most recent trade for a symbol, or empty if it never traded. */
    public synchronized OptionalDouble lastPrice(String symbol) {
        List<Trade> list = forSymbol(symbol);
        if (list.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(list.get(list.size() - 1).price());
    }
}