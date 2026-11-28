package trading;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests for the market bot's order generator. */
class MarketBotTest {

    @Test
    void ordersStayWithinTwoPercentOfReferencePrice() {
        Random rnd = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Order o = MarketBot.nextOrder(rnd, "XYZ", 10.0);
            assertTrue(o.price() >= 9.79 && o.price() <= 10.21, "price out of range: " + o.price());
        }
    }

    @Test
    void ordersAreValid() {
        Random rnd = new Random(7);
        for (int i = 0; i < 1000; i++) {
            Order o = MarketBot.nextOrder(rnd, "TORR", 20.0);
            assertTrue(o.side().equals("BUY") || o.side().equals("SELL"));
            assertTrue(o.qty() == 100 || o.qty() == 200 || o.qty() == 300);
            assertEquals("TORR", o.symbol());
            assertEquals("marketbot", o.user());
            assertFalse(o.isMarket());
        }
    }

    @Test
    void botSendsBothBuysAndSells() {
        Random rnd = new Random(1);
        boolean buy = false;
        boolean sell = false;
        for (int i = 0; i < 100; i++) {
            String side = MarketBot.nextOrder(rnd, "XYZ", 10.0).side();
            buy |= side.equals("BUY");
            sell |= side.equals("SELL");
        }
        assertTrue(buy && sell);
    }

    @Test
    void botOrdersCanMatchInTheOrderBook() {
        java.util.List<Trade> trades = new java.util.ArrayList<>();
        OrderBook book = new OrderBook(trades::add);
        Random rnd = new Random(3);
        for (int i = 0; i < 200; i++) {
            book.handle(MarketBot.nextOrder(rnd, "XYZ", 10.0));
        }
        assertFalse(trades.isEmpty());
    }
}