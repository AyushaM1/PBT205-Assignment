package trading;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Exchange {
    private static final int DEPTH_LEVELS = 5;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(port);

        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        Channel bookChannel = conn.createChannel();
        channel.queueDeclare("orders", true, false, false, null);
        channel.exchangeDeclare("trades", BuiltinExchangeType.FANOUT, true);
        bookChannel.exchangeDeclare("book", BuiltinExchangeType.FANOUT, true);

        Gson gson = new Gson();
        Persistence store = new Persistence(Path.of("."));
        Map<String, OrderBook> books = new ConcurrentHashMap<>();

        Consumer<Trade> publish = trade -> {
            try {
                store.appendTrade(trade);
            } catch (IOException e) {
                System.out.println("Could not save trade: " + e.getMessage());
            }
            try {
                channel.basicPublish("trades", "", null, gson.toJson(trade).getBytes());
            } catch (IOException e) {
                System.out.println("Could not publish trade: " + e.getMessage());
            }
        };

        List<Order> saved = store.loadOrders();
        Map<String, List<Order>> bySymbol = new ConcurrentHashMap<>();
        for (Order o : saved) {
            bySymbol.computeIfAbsent(o.symbol(), s -> new ArrayList<>()).add(o);
        }
        for (Map.Entry<String, List<Order>> e : bySymbol.entrySet()) {
            books.computeIfAbsent(e.getKey(), s -> new OrderBook(s, publish)).restore(e.getValue());
        }
        System.out.println("Restored " + saved.size() + " open orders from disk");

        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "book-publisher");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleAtFixedRate(() -> {
            try {
                for (OrderBook b : books.values()) {
                    publishBook(bookChannel, gson, b);
                }
            } catch (Exception e) {
                System.out.println("Could not publish book: " + e.getMessage());
            }
        }, 1, 2, TimeUnit.SECONDS);

        DeliverCallback onOrder = (tag, delivery) -> {
            Order order = gson.fromJson(new String(delivery.getBody()), Order.class);
            if (isValid(order)) {
                OrderBook book = books.computeIfAbsent(order.symbol(), s -> new OrderBook(s, publish));
                book.handle(order);
                saveAll(store, books);
                publishBook(bookChannel, gson, book);
            } else {
                System.out.println("REJECTED invalid order: " + new String(delivery.getBody()));
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume("orders", false, onOrder, tag -> {});
        System.out.println("Exchange waiting for orders...");
    }

    static boolean isValid(Order o) {
        if (o == null || o.user() == null || o.symbol() == null || o.side() == null) {
            return false;
        }
        if (!o.side().equals("BUY") && !o.side().equals("SELL")) {
            return false;
        }
        if (o.qty() <= 0) {
            return false;
        }
        return o.market() || o.price() > 0;
    }

    private static void saveAll(Persistence store, Map<String, OrderBook> books) {
        List<Order> all = new ArrayList<>();
        for (OrderBook b : books.values()) {
            all.addAll(b.openOrders());
        }
        try {
            store.saveOrders(all);
        } catch (IOException e) {
            System.out.println("Could not save order book: " + e.getMessage());
        }
    }

    private static void publishBook(Channel bookChannel, Gson gson, OrderBook book) throws IOException {
        byte[] body = gson.toJson(book.depth(DEPTH_LEVELS)).getBytes();
        synchronized (bookChannel) {
            bookChannel.basicPublish("book", "", null, body);
        }
    }
}
