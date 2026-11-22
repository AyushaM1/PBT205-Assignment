package trading;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.function.Consumer;

public class OrderBook {
    private static final class Resting {
        private final Order order;
        private final long seq;
        private int remaining;

        private Resting(Order order, long seq, int remaining) {
            this.order = order;
            this.seq = seq;
            this.remaining = remaining;
        }
    }

    private final String symbol;
    private final Consumer<Trade> onTrade;
    private long counter = 0;

    private final PriorityQueue<Resting> buys = new PriorityQueue<>(
        Comparator.comparingDouble((Resting r) -> r.order.price()).reversed()
                  .thenComparingLong((Resting r) -> r.seq));

    private final PriorityQueue<Resting> sells = new PriorityQueue<>(
        Comparator.comparingDouble((Resting r) -> r.order.price())
                  .thenComparingLong((Resting r) -> r.seq));

    public OrderBook(String symbol, Consumer<Trade> onTrade) {
        this.symbol = symbol;
        this.onTrade = onTrade;
    }

    public synchronized void handle(Order order) {
        boolean isBuy = order.side().equals("BUY");
        PriorityQueue<Resting> opposite = isBuy ? sells : buys;
        int left = order.qty();

        while (left > 0) {
            Resting best = opposite.peek();
            if (best == null) {
                break;
            }
            boolean crosses = order.market()
                || (isBuy ? best.order.price() <= order.price() : best.order.price() >= order.price());
            if (!crosses) {
                break;
            }
            int qty = Math.min(left, best.remaining);
            String buyer = isBuy ? order.user() : best.order.user();
            String seller = isBuy ? best.order.user() : order.user();
            executeTrade(order.symbol(), qty, best.order.price(), buyer, seller);
            left -= qty;
            best.remaining -= qty;
            if (best.remaining == 0) {
                opposite.poll();
            }
        }

        if (left == 0) {
            return;
        }
        if (order.market()) {
            System.out.println("BOOK: market " + order.side() + " " + order.user()
                + " has " + left + " unfilled (no liquidity)");
            return;
        }
        Resting rest = new Resting(order, counter++, left);
        (isBuy ? buys : sells).add(rest);
        System.out.println("BOOK: waiting " + order.side() + " " + order.user()
            + " @ " + order.price() + " (qty " + left + ")");
    }

    public synchronized List<Order> openOrders() {
        List<Resting> all = new ArrayList<>(buys);
        all.addAll(sells);
        all.sort(Comparator.comparingLong((Resting r) -> r.seq));
        List<Order> out = new ArrayList<>();
        for (Resting r : all) {
            out.add(new Order(r.order.user(), r.order.side(), r.order.symbol(), r.remaining, r.order.price()));
        }
        return out;
    }

    public synchronized void restore(List<Order> orders) {
        for (Order o : orders) {
            Resting rest = new Resting(o, counter++, o.qty());
            (o.side().equals("BUY") ? buys : sells).add(rest);
        }
    }

    public synchronized BookSnapshot depth(int levels) {
        return new BookSnapshot(symbol, levelsOf(buys, levels, true), levelsOf(sells, levels, false));
    }

    private List<BookSnapshot.Level> levelsOf(PriorityQueue<Resting> queue, int levels, boolean descending) {
        TreeMap<Double, Integer> byPrice = new TreeMap<>(
            descending ? Comparator.<Double>reverseOrder() : Comparator.<Double>naturalOrder());
        for (Resting r : queue) {
            byPrice.merge(r.order.price(), r.remaining, Integer::sum);
        }
        List<BookSnapshot.Level> out = new ArrayList<>();
        for (Map.Entry<Double, Integer> e : byPrice.entrySet()) {
            if (out.size() == levels) {
                break;
            }
            out.add(new BookSnapshot.Level(e.getKey(), e.getValue()));
        }
        return out;
    }

    private void executeTrade(String sym, int qty, double price, String buyer, String seller) {
        Trade trade = new Trade(sym, qty, price, buyer, seller, System.currentTimeMillis());
        System.out.println("TRADE: " + sym + " " + qty + " @ " + price
            + " buyer=" + buyer + " seller=" + seller);
        onTrade.accept(trade);
    }
}
