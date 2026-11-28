package trading;

/**
 * A buy or sell order. type is "LIMIT" or "MARKET". Old messages without a type
 * are treated as LIMIT, so existing senders keep working.
 */
public record Order(String user, String side, String symbol, int qty, double price, String type) {

    /** Limit order (keeps the original 5-argument constructor working). */
    public Order(String user, String side, String symbol, int qty, double price) {
        this(user, side, symbol, qty, price, "LIMIT");
    }

    /** Market order: no price, trades at the best available price. */
    public static Order market(String user, String side, String symbol, int qty) {
        return new Order(user, side, symbol, qty, 0, "MARKET");
    }

    public boolean isMarket() {
        return "MARKET".equals(type);
    }
}