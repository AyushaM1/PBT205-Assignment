package trading;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Random;

/**
 * Market bot: sends random limit orders close to a reference price so the market
 * always has activity. Usage: MarketBot <port> [symbol] [refPrice] [count] [delayMs]
 */
public class MarketBot {
    static final String BOT_NAME = "marketbot";

    /** Builds one random order within +/- 2% of the reference price, 100 to 300 shares. */
    public static Order nextOrder(Random rnd, String symbol, double refPrice) {
        String side = rnd.nextBoolean() ? "BUY" : "SELL";
        double moved = refPrice * (1 + (rnd.nextDouble() - 0.5) * 0.04);
        double price = Math.round(moved * 100.0) / 100.0;
        int qty = 100 * (1 + rnd.nextInt(3));
        return new Order(BOT_NAME, side, symbol, qty, price);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: MarketBot <port> [symbol] [refPrice] [count] [delayMs]");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String symbol = args.length > 1 ? args[1].toUpperCase() : "XYZ";
        double ref = args.length > 2 ? Double.parseDouble(args[2]) : 10.0;
        int count = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        long delay = args.length > 4 ? Long.parseLong(args[4]) : 1000;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(port);

        Random rnd = new Random();
        Gson gson = new Gson();
        try (Connection conn = factory.newConnection();
             Channel channel = conn.createChannel()) {
            channel.queueDeclare("orders", true, false, false, null);
            for (int i = 0; i < count; i++) {
                String json = gson.toJson(nextOrder(rnd, symbol, ref));
                channel.basicPublish("", "orders", null, json.getBytes());
                System.out.println("Bot sent: " + json);
                Thread.sleep(delay);
            }
        }
    }
}