package trading;

import java.util.Random;

public final class BotLogic {
    public static final int MAX_OPEN = 5;
    public static final double MIN_PRICE = 0.01;
    public static final double MIN_SPREAD = 0.10;
    public static final double MAX_SPREAD = 0.50;

    private BotLogic() {}

    public static double quotePrice(double last, String side, double spread) {
        double price = side.equals("BUY") ? last - spread : last + spread;
        return Math.max(MIN_PRICE, Math.round(price * 100.0) / 100.0);
    }

    public static boolean canQuote(int openOrders) {
        return openOrders < MAX_OPEN;
    }

    public static double randomSpread(Random rnd) {
        return Math.round((MIN_SPREAD + rnd.nextInt(5) * 0.10) * 100.0) / 100.0;
    }
}
