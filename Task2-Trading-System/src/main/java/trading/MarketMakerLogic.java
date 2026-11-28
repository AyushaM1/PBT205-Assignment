package trading;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MarketMakerLogic {
    public static final int MAX_RESTING = 600;
    public static final double MIN_PRICE = 0.01;
    public static final double MIN_SPREAD = 0.05;
    public static final double TAKE_CHANCE = 0.35;

    private MarketMakerLogic() {}

    public static double startPrice(String symbol) {
        return switch (symbol) {
            case "XYZ" -> 10.0;
            case "TORR" -> 20.0;
            default -> 10.0;
        };
    }

    public static double spread(double last, Random rnd) {
        double s = Math.round(last * (0.005 + rnd.nextDouble() * 0.015) * 100.0) / 100.0;
        return Math.max(MIN_SPREAD, s);
    }

    public static double bidPrice(double last, double spread) {
        return Math.max(MIN_PRICE, Math.round((last - spread) * 100.0) / 100.0);
    }

    public static double askPrice(double last, double spread) {
        return Math.max(MIN_PRICE, Math.round((last + spread) * 100.0) / 100.0);
    }

    public static int quoteQty(Random rnd) {
        return 50 * (1 + rnd.nextInt(4));
    }

    public static int takerQty(Random rnd) {
        return 50 * (1 + rnd.nextInt(6));
    }

    public static boolean canQuote(int restingQty, int newQty) {
        return restingQty + newQty <= MAX_RESTING;
    }

    public static boolean shouldTake(Random rnd) {
        return rnd.nextDouble() < TAKE_CHANCE;
    }

    public static List<Order> tick(Random rnd, String user, String symbol, double last,
                                   int restingBuy, int restingSell) {
        List<Order> out = new ArrayList<>();
        double spread = spread(last, rnd);

        int bidQty = quoteQty(rnd);
        if (canQuote(restingBuy, bidQty)) {
            out.add(new Order(user, "BUY", symbol, bidQty, bidPrice(last, spread)));
        }
        int askQty = quoteQty(rnd);
        if (canQuote(restingSell, askQty)) {
            out.add(new Order(user, "SELL", symbol, askQty, askPrice(last, spread)));
        }
        if (shouldTake(rnd)) {
            String side = rnd.nextBoolean() ? "BUY" : "SELL";
            out.add(Order.market(takerName(user), side, symbol, takerQty(rnd)));
        }
        return out;
    }

    public static String takerName(String user) {
        return user + "_taker";
    }
}
