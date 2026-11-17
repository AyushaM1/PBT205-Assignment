package trading;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for partial fills (FR5) and market orders (FR4). */
class OrderBookExtensionTest {
    private List<Trade> trades;
    private OrderBook book;

    @BeforeEach
    void setUp() {
        trades = new ArrayList<>();
        book = new OrderBook(trades::add);
    }

    private Order limit(String user, String side, int qty, double price) {
        return new Order(user, side, "XYZ", qty, price);
    }

    // ---- FR5: partial fills ----

    @Test
    void bigBuyPartlyFilledByOneSmallSell() {
        book.handle(limit("seller", "SELL", 100, 10.00));
        book.handle(limit("buyer", "BUY", 300, 10.00));
        assertEquals(1, trades.size());
        assertEquals(100, trades.get(0).qty());
    }

    @Test
    void remainderStaysOnBookAndMatchesLater() {
        book.handle(limit("seller", "SELL", 100, 10.00));
        book.handle(limit("buyer", "BUY", 300, 10.00));   // 200 left waiting at 10.00
        book.handle(limit("seller2", "SELL", 200, 9.50)); // trades against the waiting buy
        assertEquals(2, trades.size());
        assertEquals(200, trades.get(1).qty());
        assertEquals(10.00, trades.get(1).price()); // resting buy price
        assertEquals("buyer", trades.get(1).buyer());
    }

    @Test
    void restingOrderPartlyFilledKeepsItsPlaceInQueue() {
        book.handle(limit("seller", "SELL", 300, 10.00));
        book.handle(limit("b1", "BUY", 100, 10.00));
        book.handle(limit("b2", "BUY", 100, 10.00));
        assertEquals(2, trades.size());
        assertEquals("seller", trades.get(1).seller());
        assertEquals(100, trades.get(1).qty());
    }

    @Test
    void incomingOrderSweepsSeveralPriceLevels() {
        book.handle(limit("a", "SELL", 100, 10.00));
        book.handle(limit("b", "SELL", 100, 10.50));
        book.handle(limit("c", "SELL", 100, 11.50)); // above the buyer's limit
        book.handle(limit("buyer", "BUY", 250, 11.00));
        assertEquals(2, trades.size());
        assertEquals(10.00, trades.get(0).price());
        assertEquals(10.50, trades.get(1).price());
        assertEquals(100, trades.get(0).qty());
        assertEquals(100, trades.get(1).qty());
        // 50 left waits as a BUY at 11.00 and does not touch the 11.50 sell
        book.handle(limit("d", "SELL", 50, 11.00));
        assertEquals(3, trades.size());
        assertEquals(50, trades.get(2).qty());
    }

    @Test
    void totalTradedNeverExceedsOrderSize() {
        book.handle(limit("s", "SELL", 100, 10.00));
        book.handle(limit("b", "BUY", 250, 10.00));
        int total = trades.stream().mapToInt(Trade::qty).sum();
        assertEquals(100, total);
    }

    // ---- FR4: market orders ----

    @Test
    void marketBuyTradesAtBestSellPrice() {
        book.handle(limit("s1", "SELL", 100, 10.50));
        book.handle(limit("s2", "SELL", 100, 10.00));
        book.handle(Order.market("m", "BUY", "XYZ", 100));
        assertEquals(1, trades.size());
        assertEquals(10.00, trades.get(0).price());
        assertEquals("s2", trades.get(0).seller());
    }

    @Test
    void marketSellTradesAtBestBuyPrice() {
        book.handle(limit("b1", "BUY", 100, 9.00));
        book.handle(limit("b2", "BUY", 100, 9.50));
        book.handle(Order.market("m", "SELL", "XYZ", 100));
        assertEquals(9.50, trades.get(0).price());
        assertEquals("b2", trades.get(0).buyer());
    }

    @Test
    void marketOrderOnEmptyBookDoesNotTradeOrWait() {
        book.handle(Order.market("m", "BUY", "XYZ", 100));
        assertEquals(0, trades.size());
        // a later sell must NOT match a phantom market order
        book.handle(limit("s", "SELL", 100, 10.00));
        assertEquals(0, trades.size());
    }

    @Test
    void marketOrderSweepsLevelsAndDropsUnfilledRemainder() {
        book.handle(limit("s1", "SELL", 100, 10.00));
        book.handle(limit("s2", "SELL", 100, 10.50));
        book.handle(Order.market("m", "BUY", "XYZ", 300));
        assertEquals(2, trades.size());
        book.handle(limit("s3", "SELL", 100, 12.00));
        assertEquals(2, trades.size()); // the 100 unfilled shares did not wait
    }

    @Test
    void orderWithoutTypeFieldIsTreatedAsLimit() {
        Order old = new com.google.gson.Gson().fromJson(
            "{\"user\":\"u\",\"side\":\"SELL\",\"symbol\":\"XYZ\",\"qty\":100,\"price\":10.0}", Order.class);
        assertFalse(old.isMarket());
    }
}