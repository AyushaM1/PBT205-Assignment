package trading;

public record Order(String user, String side, String symbol, int qty, double price, boolean market) {
    public Order(String user, String side, String symbol, int qty, double price) {
        this(user, side, symbol, qty, price, false);
    }

    public static Order marketOrder(String user, String side, String symbol, int qty) {
        return new Order(user, side, symbol, qty, 0.0, true);
    }
}
