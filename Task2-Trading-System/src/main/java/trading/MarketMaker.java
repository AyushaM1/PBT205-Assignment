package trading;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Market maker bot. Keeps a bid and an ask around the last trade price for each
 * stock, and sends the occasional market order from a second "taker" trader so
 * trades happen, the price moves and partial fills show up.
 * Usage: MarketMaker <port> [user] [delayMs] [symbols, e.g. XYZ,TORR]
 */
public class MarketMaker {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: MarketMaker <port> [user] [delayMs] [symbols, e.g. XYZ,TORR]");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String user = args.length > 1 ? args[1] : "maker1";
        long delay = args.length > 2 ? Long.parseLong(args[2]) : 1000;
        List<String> symbols = args.length > 3
            ? List.of(args[3].toUpperCase().split(","))
            : List.of("XYZ", "TORR");

        Map<String, Double> last = new ConcurrentHashMap<>();
        TradeHistory past = new TradeHistory(Path.of("trades.csv"));
        for (String s : symbols) {
            last.put(s, past.lastPrice(s).orElse(MarketMakerLogic.startPrice(s)));
        }

        Map<String, AtomicInteger> resting = new ConcurrentHashMap<>();
        for (Order o : new Persistence(Path.of(".")).loadOrders()) {
            if (o.user().equals(user)) {
                resting.computeIfAbsent(o.symbol() + o.side(), k -> new AtomicInteger()).addAndGet(o.qty());
            }
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(port);

        Connection conn = factory.newConnection();
        Channel pub = conn.createChannel();
        Channel sub = conn.createChannel();
        pub.queueDeclare("orders", true, false, false, null);
        sub.exchangeDeclare("trades", BuiltinExchangeType.FANOUT, true);
        String tradeQueue = sub.queueDeclare().getQueue();
        sub.queueBind(tradeQueue, "trades", "");

        Gson gson = new Gson();
        sub.basicConsume(tradeQueue, true, (tag, delivery) -> {
            Trade t = gson.fromJson(new String(delivery.getBody(), StandardCharsets.UTF_8), Trade.class);
            if (last.containsKey(t.symbol())) {
                last.put(t.symbol(), t.price());
            }
            if (user.equals(t.buyer())) {
                release(resting, t.symbol() + "BUY", t.qty());
            }
            if (user.equals(t.seller())) {
                release(resting, t.symbol() + "SELL", t.qty());
            }
        }, tag -> {});

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }));

        Random rnd = new Random();
        System.out.println("MarketMaker running as " + user + " on " + symbols + ". Press Ctrl+C to stop.");

        while (true) {
            String symbol = symbols.get(rnd.nextInt(symbols.size()));
            int buyResting = resting.computeIfAbsent(symbol + "BUY", k -> new AtomicInteger()).get();
            int sellResting = resting.computeIfAbsent(symbol + "SELL", k -> new AtomicInteger()).get();

            for (Order order : MarketMakerLogic.tick(rnd, user, symbol, last.get(symbol), buyResting, sellResting)) {
                pub.basicPublish("", "orders", null, gson.toJson(order).getBytes(StandardCharsets.UTF_8));
                if (!order.isMarket()) {
                    resting.get(symbol + order.side()).addAndGet(order.qty());
                }
                System.out.println("MAKER: " + (order.isMarket() ? "market " : "") + order.side() + " "
                    + order.symbol() + " " + order.qty()
                    + (order.isMarket() ? "" : " @ " + order.price()));
            }
            Thread.sleep(delay / 2 + rnd.nextInt((int) Math.max(1, delay)));
        }
    }

    private static void release(Map<String, AtomicInteger> resting, String key, int qty) {
        AtomicInteger count = resting.get(key);
        if (count != null) {
            count.updateAndGet(v -> Math.max(0, v - qty));
        }
    }
}
