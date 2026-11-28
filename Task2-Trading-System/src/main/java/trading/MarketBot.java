package trading;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketBot {
    private static final List<String> SYMBOLS = List.of("XYZ", "TORR");
    private static final List<String> SIDES = List.of("BUY", "SELL");

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5672;
        String user = args.length > 1 ? args[1] : "bot1";

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
        Map<String, Double> last = new ConcurrentHashMap<>();
        last.put("XYZ", 10.0);
        last.put("TORR", 20.0);
        Map<String, AtomicInteger> open = new ConcurrentHashMap<>();

        sub.basicConsume(tradeQueue, true, (tag, delivery) -> {
            Trade trade = gson.fromJson(new String(delivery.getBody(), StandardCharsets.UTF_8), Trade.class);
            last.put(trade.symbol(), trade.price());
            if (user.equals(trade.buyer())) {
                open.computeIfAbsent(trade.symbol() + "BUY", k -> new AtomicInteger())
                    .updateAndGet(v -> Math.max(0, v - 1));
            }
            if (user.equals(trade.seller())) {
                open.computeIfAbsent(trade.symbol() + "SELL", k -> new AtomicInteger())
                    .updateAndGet(v -> Math.max(0, v - 1));
            }
        }, tag -> {});

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }));

        Random rnd = new Random();
        System.out.println("MarketBot running as " + user + ". Press Ctrl+C to stop.");

        while (true) {
            String symbol = SYMBOLS.get(rnd.nextInt(SYMBOLS.size()));
            String side = SIDES.get(rnd.nextInt(SIDES.size()));
            AtomicInteger count = open.computeIfAbsent(symbol + side, k -> new AtomicInteger());

            if (BotLogic.canQuote(count.get())) {
                double price = BotLogic.quotePrice(last.get(symbol), side, BotLogic.randomSpread(rnd));
                int qty = 50 + 50 * rnd.nextInt(4);
                Order order = new Order(user, side, symbol, qty, price);
                pub.basicPublish("", "orders", null, gson.toJson(order).getBytes(StandardCharsets.UTF_8));
                count.incrementAndGet();
                System.out.println("BOT: " + side + " " + symbol + " " + qty + " @ " + price);
            }
            Thread.sleep(1000 + rnd.nextInt(2000));
        }
    }
}
