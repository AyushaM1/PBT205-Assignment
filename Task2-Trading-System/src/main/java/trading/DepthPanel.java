package trading;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
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
        panel.add(new JScrollPane(createStyledTable(m.bids, true)));
        panel.add(new JScrollPane(createStyledTable(m.asks, false)));
        add(panel, BorderLayout.CENTER);
        revalidate();
        return m;
    }

    private static JTable createStyledTable(
            DefaultTableModel model, boolean isBid) {

        JTable table = new JTable(model);

        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setBackground(new Color(30, 41, 59));
        table.setForeground(new Color(248, 250, 252));

        Color accent = isBid
                ? new Color(16, 185, 129)
                : new Color(244, 63, 94);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value,
                    boolean selected, boolean focused,
                    int row, int column) {

                super.getTableCellRendererComponent(
                        t, value, selected, focused, row, column);

                setForeground(column == 0
                        ? accent
                        : new Color(248, 250, 252));

                setBackground(selected
                        ? new Color(51, 65, 85)
                        : new Color(30, 41, 59));

                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i)
                    .setCellRenderer(renderer);
        }

        return table;
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
