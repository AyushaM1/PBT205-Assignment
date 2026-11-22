package trading;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class DepthPanel extends JPanel {
    private static final class Models {
        private final DefaultTableModel bids = new DefaultTableModel(new Object[] {"Bid price", "Bid qty"}, 0);
        private final DefaultTableModel asks = new DefaultTableModel(new Object[] {"Ask price", "Ask qty"}, 0);
    }

    private final Map<String, Models> bySymbol = new HashMap<>();

    public DepthPanel(List<String> symbols) {
        super(new GridLayout(0, 1, 0, 6));
        for (String s : symbols) {
            ensure(s);
        }
    }

    private Models ensure(String symbol) {
        Models existing = bySymbol.get(symbol);
        if (existing != null) {
            return existing;
        }
        Models m = new Models();
        bySymbol.put(symbol, m);
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
        panel.setBorder(BorderFactory.createTitledBorder(symbol + " order book"));
        panel.add(new JScrollPane(new JTable(m.bids)));
        panel.add(new JScrollPane(new JTable(m.asks)));
        add(panel, BorderLayout.CENTER);
        revalidate();
        return m;
    }

    public void update(BookSnapshot snap) {
        Models m = ensure(snap.symbol());
        fill(m.bids, snap.bids());
        fill(m.asks, snap.asks());
    }

    private static void fill(DefaultTableModel model, List<BookSnapshot.Level> levels) {
        model.setRowCount(0);
        if (levels == null) {
            return;
        }
        for (BookSnapshot.Level l : levels) {
            model.addRow(new Object[] {l.price(), l.qty()});
        }
    }
}
