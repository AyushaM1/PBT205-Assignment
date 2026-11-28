package trading;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for the trading history (FR2) and the price checker. */
class TradeHistoryTest {
    private Path file;

    @BeforeEach
    void setUp() throws IOException {
        file = Files.createTempFile("trades", ".csv");
        Files.delete(file); // start with no file, like a first run
        file.toFile().deleteOnExit();
    }

    private Trade trade(String symbol, int qty, double price, String buyer, String seller) {
        return new Trade(symbol, qty, price, buyer, seller, 1000L);
    }

    @Test
    void historySurvivesRestart() {
        TradeHistory first = new TradeHistory(file);
        first.record(trade("XYZ", 100, 10.0, "sam", "shishir"));
        first.record(trade("TORR", 200, 20.5, "ann", "bob"));

        TradeHistory afterRestart = new TradeHistory(file); // new object reads the file
        assertEquals(2, afterRestart.all().size());
        assertEquals(20.5, afterRestart.all().get(1).price());
        assertEquals("sam", afterRestart.all().get(0).buyer());
    }

    @Test
    void lastPriceIsTheMostRecentTrade() {
        TradeHistory h = new TradeHistory(file);
        h.record(trade("XYZ", 100, 10.0, "a", "b"));
        h.record(trade("XYZ", 100, 10.5, "c", "d"));
        assertEquals(10.5, h.lastPrice("XYZ").getAsDouble());
    }

    @Test
    void unknownSymbolHasNoPrice() {
        TradeHistory h = new TradeHistory(file);
        h.record(trade("XYZ", 100, 10.0, "a", "b"));
        assertTrue(h.lastPrice("TORR").isEmpty());
    }

    @Test
    void symbolsAreKeptSeparate() {
        TradeHistory h = new TradeHistory(file);
        h.record(trade("XYZ", 100, 10.0, "a", "b"));
        h.record(trade("TORR", 100, 20.0, "c", "d"));
        assertEquals(1, h.forSymbol("XYZ").size());
        assertEquals(1, h.forSymbol("torr").size());
    }

    @Test
    void headerIsWrittenOnlyOnce() throws IOException {
        TradeHistory h = new TradeHistory(file);
        h.record(trade("XYZ", 100, 10.0, "a", "b"));
        h.record(trade("XYZ", 100, 11.0, "c", "d"));
        long headers = Files.readAllLines(file).stream().filter(l -> l.startsWith("time,")).count();
        assertEquals(1, headers);
    }

    @Test
    void badLineInFileIsSkippedNotFatal() throws IOException {
        Files.writeString(file, "time,symbol,qty,price,buyer,seller\n"
            + "this is not a trade\n"
            + "1000,XYZ,100,10.0,a,b\n");
        TradeHistory h = new TradeHistory(file);
        assertEquals(1, h.all().size());
    }

    @Test
    void priceCheckerReportsPriceAndMissingSymbol() {
        TradeHistory h = new TradeHistory(file);
        h.record(trade("XYZ", 100, 10.0, "a", "b"));
        assertTrue(PriceChecker.price(h, "xyz").contains("10.0"));
        assertTrue(PriceChecker.price(h, "TORR").contains("no trades"));
    }

    @Test
    void historyViewerListsTrades() {
        TradeHistory h = new TradeHistory(file);
        h.record(trade("XYZ", 100, 10.0, "sam", "shishir"));
        assertTrue(PriceChecker.history(h, null).contains("buyer=sam"));
        assertEquals("No trades in history", PriceChecker.history(h, "TORR"));
    }
}