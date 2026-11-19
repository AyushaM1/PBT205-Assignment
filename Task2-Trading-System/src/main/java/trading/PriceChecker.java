package trading;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Stock price checker and trading history viewer. Reads trades.csv, so it works
 * even when the Exchange is not running.
 *
 * Usage: PriceChecker price SYMBOL [file]
 *        PriceChecker history [SYMBOL] [file]
 */
public class PriceChecker {

    public static String price(TradeHistory h, String symbol) {
        OptionalDouble p = h.lastPrice(symbol);
        if (p.isEmpty()) {
            return symbol.toUpperCase() + ": no trades yet";
        }
        return symbol.toUpperCase() + " last price: " + p.getAsDouble()
            + " (" + h.forSymbol(symbol).size() + " trades)";
    }

    public static String history(TradeHistory h, String symbol) {
        List<Trade> list = symbol == null ? h.all() : h.forSymbol(symbol);
        if (list.isEmpty()) {
            return "No trades in history";
        }
        StringBuilder sb = new StringBuilder();
        for (Trade t : list) {
            sb.append(t.symbol()).append(' ').append(t.qty()).append(" @ ").append(t.price())
              .append(" buyer=").append(t.buyer()).append(" seller=").append(t.seller())
              .append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: PriceChecker price SYMBOL [file] | PriceChecker history [SYMBOL] [file]");
            return;
        }
        String cmd = args[0].toLowerCase();
        if (cmd.equals("price") && args.length >= 2) {
            Path file = Path.of(args.length > 2 ? args[2] : "trades.csv");
            System.out.println(price(new TradeHistory(file), args[1]));
        } else if (cmd.equals("history")) {
            String symbol = args.length > 1 ? args[1] : null;
            Path file = Path.of(args.length > 2 ? args[2] : "trades.csv");
            System.out.println(history(new TradeHistory(file), symbol));
        } else {
            System.out.println("Usage: PriceChecker price SYMBOL [file] | PriceChecker history [SYMBOL] [file]");
        }
    }
}