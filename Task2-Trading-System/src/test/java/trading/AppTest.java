package trading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void savedOrdersComeBackAfterRestart() throws Exception {
        Path dir = Files.createTempDirectory("persist-test");
        List<Order> orders = List.of(
                new Order("ayusha", "BUY", "XYZ", 300, 10.0),
                new Order("shishir", "SELL", "XYZ", 200, 10.5));

        new Persistence(dir).saveOrders(orders);

        // a brand new Persistence object acts like a restarted exchange
        List<Order> loaded = new Persistence(dir).loadOrders();

        assertEquals(orders, loaded);
    }
}