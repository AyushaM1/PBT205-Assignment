package trading;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regression tests for the existing limit-order matching rules (requirement FR3). */
class OrderBookTest {
    private List<Trade> trades;
    private OrderBook book;

    @BeforeEach
    void setUp() {
        trades = new ArrayList<>();
        book = new OrderBook(trades::add);
    }

    private Order sell(String user, double price) { return new Order(user, "SELL", "XYZ", 100, price); }
    private Order buy(String user, double price)  { return new Order(user, "BUY",  "XYZ", 100, price); }

    @Test // Test 1: SELL 10.00, BUY 9.00 -> no trade
    void noTradeWhenBuyBelowSell() {
        book.handle(sell("shishir", 10.00));
        book.handle(buy("sam", 9.00));
        assertEquals(0, trades.size());
    }

    @Test // Test 2: equal prices trade
    void tradesWhenPricesEqual() {
        book.handle(sell("shishir", 10.00));
        book.handle(buy("sam", 10.00));
        assertEquals(1, trades.size());
        assertEquals(10.00, trades.get(0).price());
    }

    @Test // Test 3: trade at the resting (waiting) order's price
    void tradesAtRestingSellPrice() {
        book.handle(sell("shishir", 10.00));
        book.handle(buy("sam", 11.00));
        assertEquals(10.00, trades.get(0).price());
        assertEquals("sam", trades.get(0).buyer());
        assertEquals("shishir", trades.get(0).seller());
    }

    @Test // Test 4: lowest sell matches first
    void lowestSellMatchesFirst() {
        book.handle(sell("a", 10.00));
        book.handle(sell("b", 10.50));
        book.handle(buy("c", 11.00));
        book.handle(buy("d", 10.50));
        assertEquals(2, trades.size());
        assertEquals(10.00, trades.get(0).price());
        assertEquals(10.50, trades.get(1).price());
    }

    @Test // Test 5: highest buy matches first, at the resting price
    void highestBuyMatchesFirst() {
        book.handle(buy("a", 11.00));
        book.handle(buy("b", 10.00));
        book.handle(sell("c", 9.50));
        assertEquals(11.00, trades.get(0).price());
        assertEquals("a", trades.get(0).buyer());
    }

    @Test // Ties go to the earlier order
    void earlierOrderWinsTie() {
        book.handle(sell("first", 10.00));
        book.handle(sell("second", 10.00));
        book.handle(buy("sam", 10.00));
        assertEquals("first", trades.get(0).seller());
    }

    @Test // A matched order must be removed from the book
    void matchedOrderLeavesBook() {
        book.handle(sell("shishir", 10.00));
        book.handle(buy("sam", 10.00));
        book.handle(buy("tom", 10.00));
        assertEquals(1, trades.size());
    }
}