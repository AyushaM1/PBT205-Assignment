package trading;

import java.util.List;

public record BookSnapshot(String symbol, List<Level> bids, List<Level> asks) {
    public record Level(double price, int qty) {
    }
}
