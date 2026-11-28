package trading;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Order book for one stock. Supports limit orders, market orders (FR4) and
 * partial fills (FR5). Trades happen at the price of the order already waiting (FR3).
 */
public class OrderBook {
    /** A waiting order. remaining shrinks as it is partly filled. */
    private static final class Resting {
        final Order order;
        final long seq;
        int remaining;

        Resting(Order order, long seq, int remaining) {
            this.order = order;
            this.seq = seq;
            this.remaining = remaining;
        }
    }

    private long counter = 0;
    private final Consumer<Trade> onTrade;

    private final PriorityQueue<Resting> buys = new PriorityQueue<>(
        Comparator.comparingDouble((Resting r) -> r.order.price()).reversed()
                  .thenComparingLong(r -> r.seq));

    private final PriorityQueue<Resting> sells = new PriorityQueue<>(
        Comparator.comparingDouble((Resting r) -> r.order.price())
                  .thenComparingLong(r -> r.seq));

    public OrderBook(Consumer<Trade> onTrade) {
        this.onTrade = onTrade;
    }

    public void handle(Order order) {
        boolean isBuy = order.side().equals("BUY");
        PriorityQueue<Resting> opposite = isBuy ? sells : buys;
        int remaining = order.qty();

        while (remaining > 0) {
            Resting best = opposite.peek();
            if (best == null || !crosses(order, best.order, isBuy)) {
                break;
            }
            int fill = Math.min(remaining, best.remaining);
            double price = best.order.price();
            if (isBuy) {
                executeTrade(order.symbol(), fill, price, order.user(), best.order.user());
            } else {
                executeTrade(order.symbol(), fill, price, best.order.user(), order.user());
            }
            remaining -= fill;
            best.remaining -= fill;
            if (best.remaining == 0) {
                opposite.poll();
            }
        }

        if (remaining == 0) {
            return;
        }
        if (order.isMarket()) {
            // A market order has no price, so it cannot wait on the book.
            System.out.println("MARKET: " + remaining + " unfilled for " + order.user()
                + " (no liquidity), cancelled");
            return;
        }
        Resting rest = new Resting(order, counter++, remaining);
        (isBuy ? buys : sells).add(rest);
        System.out.println("BOOK: waiting " + order.side() + " " + order.user() + " "
            + remaining + " @ " + order.price());
    }

    /** True if the incoming order can trade against the best waiting order. */
    private boolean crosses(Order incoming, Order waiting, boolean incomingIsBuy) {
        if (incoming.isMarket()) {
            return true;
        }
        return incomingIsBuy ? waiting.price() <= incoming.price()
                             : waiting.price() >= incoming.price();
    }

    private void executeTrade(String symbol, int qty, double price, String buyer, String seller) {
        Trade trade = new Trade(symbol, qty, price, buyer, seller, System.currentTimeMillis());
        System.out.println("TRADE: " + symbol + " " + qty + " @ " + price
            + " buyer=" + buyer + " seller=" + seller);
        onTrade.accept(trade);
    }
}