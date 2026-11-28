package trading;

public final class OrderFormLogic {
    public static final int MAX_QTY = 1_000_000;
    public static final double MAX_PRICE = 1_000_000;

    private OrderFormLogic() {}

    public static Order build(String user, String side, String symbol, int qty,
                              boolean market, String priceText) {
        String name = user == null ? "" : user.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter a username");
        }
        if (name.length() > 20) {
            throw new IllegalArgumentException("Username must be 20 characters or fewer");
        }
        if (name.contains(",")) {
            throw new IllegalArgumentException("Username cannot contain commas");
        }

        String sym = symbol == null ? "" : symbol.trim().toUpperCase();
        if (!sym.matches("[A-Z0-9]{1,6}")) {
            throw new IllegalArgumentException("Symbol must be 1 to 6 letters or digits");
        }
        if (!"BUY".equals(side) && !"SELL".equals(side)) {
            throw new IllegalArgumentException("Choose BUY or SELL");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be above 0");
        }
        if (qty > MAX_QTY) {
            throw new IllegalArgumentException("Quantity is too large (maximum " + MAX_QTY + ")");
        }
        if (market) {
            return Order.market(name, side, sym, qty);
        }

        double price;
        try {
            price = Double.parseDouble(priceText == null ? "" : priceText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid price");
        }
        if (Double.isNaN(price) || Double.isInfinite(price)) {
            throw new IllegalArgumentException("Enter a valid price");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be above 0");
        }
        if (price > MAX_PRICE) {
            throw new IllegalArgumentException("Price is too high");
        }
        return new Order(name, side, sym, qty, Math.round(price * 100.0) / 100.0);
    }
}
