package trading;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class OrderEntryPanel extends JPanel {
    @FunctionalInterface
    public interface OrderSender {
        void send(Order order) throws Exception;
    }

    private static final Color BACKGROUND = new Color(16, 24, 39);
    private static final Color CARD = new Color(30, 41, 59);
    private static final Color FIELD = new Color(51, 65, 85);
    private static final Color TEXT = new Color(248, 250, 252);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color GREEN = new Color(16, 185, 129);
    private static final Color RED = new Color(244, 63, 94);
    private static final Color BLUE = new Color(56, 189, 248);
    private static final int MAX_LOG_ROWS = 50;

    private final JTextField userField = new JTextField("trader1");
    private final JComboBox<String> symbolBox;
    private final ChoiceButton buyButton = new ChoiceButton("BUY", GREEN);
    private final ChoiceButton sellButton = new ChoiceButton("SELL", RED);
    private final ChoiceButton limitButton = new ChoiceButton("LIMIT", BLUE);
    private final ChoiceButton marketButton = new ChoiceButton("MARKET", BLUE);
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(100, 1, OrderFormLogic.MAX_QTY, 50));
    private final JTextField priceField = new JTextField("10.00");
    private final JButton useLastButton = new JButton("Use last");
    private final JLabel lastLabel = new JLabel("Last price: none yet");
    private final ActionButton submitButton = new ActionButton("Place BUY order");
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel logModel = new DefaultTableModel(
        new Object[] {"Time", "Side", "Symbol", "Qty", "Price", "Result"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final Map<String, Double> lastPrices = new HashMap<>();
    private OrderSender sender;

    public OrderEntryPanel(List<String> symbols) {
        super(new BorderLayout(16, 0));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        symbolBox = new JComboBox<>(symbols.toArray(new String[0]));
        symbolBox.setEditable(true);

        add(buildForm(), BorderLayout.WEST);
        add(buildLog(), BorderLayout.CENTER);
        wireEvents();
        buyButton.setSelected(true);
        limitButton.setSelected(true);
        refreshState();
    }

    public void setSender(OrderSender sender) {
        this.sender = sender;
    }

    public void setLastPrice(String symbol, double price) {
        lastPrices.put(symbol, price);
        refreshLastLabel();
    }

    private JPanel buildForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        card.setPreferredSize(new Dimension(380, 520));

        styleField(userField);
        styleField(priceField);
        styleCombo(symbolBox);
        styleSpinner(qtySpinner);

        useLastButton.setFocusPainted(false);
        useLastButton.setBackground(FIELD);
        useLastButton.setForeground(TEXT);
        useLastButton.setBorder(new EmptyBorder(6, 10, 6, 10));

        lastLabel.setForeground(MUTED);
        lastLabel.setFont(lastLabel.getFont().deriveFont(Font.PLAIN, 12f));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 13f));

        ButtonGroup sideGroup = new ButtonGroup();
        sideGroup.add(buyButton);
        sideGroup.add(sellButton);
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(limitButton);
        typeGroup.add(marketButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        c.gridy = 0;

        JLabel title = new JLabel("NEW ORDER");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        card.add(title, c);

        c.gridy++;
        JLabel sub = new JLabel("Send an order straight to the Exchange");
        sub.setForeground(MUTED);
        c.insets = new Insets(0, 0, 16, 0);
        card.add(sub, c);

        c.insets = new Insets(8, 0, 3, 0);
        c.gridy++;
        card.add(label("Username"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        card.add(userField, c);

        c.insets = new Insets(10, 0, 3, 0);
        c.gridy++;
        card.add(label("Symbol"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        card.add(symbolBox, c);

        c.insets = new Insets(10, 0, 3, 0);
        c.gridy++;
        card.add(label("Side"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        card.add(pair(buyButton, sellButton), c);

        c.insets = new Insets(10, 0, 3, 0);
        c.gridy++;
        card.add(label("Order type"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        card.add(pair(limitButton, marketButton), c);

        c.insets = new Insets(10, 0, 3, 0);
        c.gridy++;
        card.add(label("Quantity (shares)"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        card.add(qtySpinner, c);

        c.insets = new Insets(10, 0, 3, 0);
        c.gridy++;
        card.add(label("Limit price"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 1;
        card.add(priceField, c);
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 8, 0, 0);
        card.add(useLastButton, c);

        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 0, 0);
        c.gridy++;
        card.add(lastLabel, c);

        c.insets = new Insets(18, 0, 0, 0);
        c.gridy++;
        submitButton.setPreferredSize(new Dimension(100, 46));
        card.add(submitButton, c);

        c.insets = new Insets(10, 0, 0, 0);
        c.gridy++;
        card.add(status, c);

        c.gridy++;
        c.weighty = 1;
        card.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, c);
        return card;
    }

    private JScrollPane buildLog() {
        JTable table = new JTable(logModel);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setBackground(CARD);
        table.setForeground(TEXT);
        table.getTableHeader().setBackground(CARD);
        table.getTableHeader().setForeground(MUTED);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                            boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                setBackground(selected ? FIELD : CARD);
                setHorizontalAlignment(SwingConstants.CENTER);
                String text = value == null ? "" : value.toString();
                Color color = TEXT;
                if (column == 1) {
                    color = text.equals("BUY") ? GREEN : RED;
                } else if (column == 5) {
                    color = text.startsWith("Sent") ? GREEN : RED;
                }
                setForeground(color);
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(FIELD), "Orders sent from this window",
            0, 0, null, MUTED));
        pane.getViewport().setBackground(CARD);
        pane.setViewportBorder(null);
        pane.setBackground(BACKGROUND);
        return pane;
    }

    private void wireEvents() {
        buyButton.addActionListener(e -> refreshState());
        sellButton.addActionListener(e -> refreshState());
        limitButton.addActionListener(e -> refreshState());
        marketButton.addActionListener(e -> refreshState());
        symbolBox.addActionListener(e -> refreshLastLabel());
        useLastButton.addActionListener(e -> {
            Double last = lastPrices.get(selectedSymbol());
            if (last != null) {
                priceField.setText(String.format("%.2f", last));
            }
        });
        submitButton.addActionListener(e -> submit());
        userField.addActionListener(e -> submit());
        priceField.addActionListener(e -> submit());
    }

    private void refreshState() {
        boolean buy = buyButton.isSelected();
        boolean market = marketButton.isSelected();
        submitButton.setColor(buy ? GREEN : RED);
        submitButton.setText("Place " + (buy ? "BUY" : "SELL") + (market ? " market order" : " order"));
        priceField.setEnabled(!market);
        useLastButton.setEnabled(!market);
        priceField.setForeground(market ? MUTED : TEXT);
        repaint();
    }

    private void refreshLastLabel() {
        Double last = lastPrices.get(selectedSymbol());
        lastLabel.setText(last == null
            ? "Last price: none yet"
            : "Last " + selectedSymbol() + " price: " + String.format("%.2f", last));
    }

    private String selectedSymbol() {
        Object item = symbolBox.getEditor().getItem();
        return item == null ? "" : item.toString().trim().toUpperCase();
    }

    private void submit() {
        String side = buyButton.isSelected() ? "BUY" : "SELL";
        boolean market = marketButton.isSelected();
        Order order;
        try {
            order = OrderFormLogic.build(userField.getText(), side, selectedSymbol(),
                ((Number) qtySpinner.getValue()).intValue(), market, priceField.getText());
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), RED);
            return;
        }
        if (sender == null) {
            showStatus("Not connected to RabbitMQ yet", RED);
            return;
        }
        try {
            sender.send(order);
            String summary = order.side() + " " + order.qty() + " " + order.symbol()
                + (order.isMarket() ? " at market" : " @ " + String.format("%.2f", order.price()));
            showStatus("Sent: " + summary, GREEN);
            addLog(order, "Sent");
        } catch (Exception e) {
            showStatus("Could not send: " + e.getMessage(), RED);
            addLog(order, "Failed");
        }
    }

    private void addLog(Order order, String result) {
        logModel.insertRow(0, new Object[] {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            order.side(), order.symbol(), order.qty(),
            order.isMarket() ? "MARKET" : String.format("%.2f", order.price()), result});
        if (logModel.getRowCount() > MAX_LOG_ROWS) {
            logModel.removeRow(logModel.getRowCount() - 1);
        }
    }

    private void showStatus(String text, Color color) {
        status.setText(text);
        status.setForeground(color);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        return l;
    }

    private static JPanel pair(AbstractButton a, AbstractButton b) {
        JPanel p = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        p.setOpaque(false);
        a.setPreferredSize(new Dimension(100, 38));
        b.setPreferredSize(new Dimension(100, 38));
        p.add(a);
        p.add(b);
        return p;
    }

    private static void styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setDisabledTextColor(MUTED);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD.brighter()), new EmptyBorder(8, 10, 8, 10)));
    }

    private static void styleCombo(JComboBox<String> box) {
        box.setBackground(FIELD);
        box.setForeground(TEXT);
        Component editor = box.getEditor().getEditorComponent();
        if (editor instanceof JTextField field) {
            styleField(field);
        }
    }

    private static void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createLineBorder(FIELD.brighter()));
        Component editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor def) {
            styleField(def.getTextField());
        }
    }

    private static final class ChoiceButton extends JToggleButton {
        private final Color active;

        ChoiceButton(String text, Color active) {
            super(text);
            this.active = active;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isSelected() ? active : FIELD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            setForeground(isSelected() ? Color.WHITE : MUTED);
            super.paintComponent(g);
        }
    }

    private static final class ActionButton extends JButton {
        private Color color = GREEN;

        ActionButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 15f));
        }

        void setColor(Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? color.darker() : color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
