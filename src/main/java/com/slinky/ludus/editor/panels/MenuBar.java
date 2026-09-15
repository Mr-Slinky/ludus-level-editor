package com.slinky.ludus.editor.panels;

import com.slinky.ludus.editor.data.Palette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A row of text items, each of which runs its own action when pressed. The selected item draws in the light
 * tone above an underline in the accent that a palette gives to a chosen value, and the other items draw in a
 * muted tone.
 * <p>
 * {@link #addItem(String, Runnable)} appends one item to the right of the others. The first item added starts
 * selected without running its action, so a caller shows that item's content before the menu goes on screen.
 * {@link #selectItem(int)} selects an item and runs its action, and a press on an item calls it.
 * <p>
 * <b>Switching between two cards</b>
 * <p>
 * A caller adds one item per card of a {@link java.awt.CardLayout}, and puts the menu in a title bar:
 * <pre>{@code
 * var menu = new MenuBar();
 *
 * menu.addItem("Tiles",    () -> cards.show(deck, "Tiles"));
 * menu.addItem("Metadata", () -> cards.show(deck, "Metadata"));
 * titleBar.addLeading(menu);
 *
 * menu.getSelectedIndex();  // 0
 * menu.selectItem(1);       // shows the "Metadata" card
 * menu.getSelectedIndex();  // 1
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *         <p>
 *         Last modified: 2026-09-10
 * @since 1.0.0
 */
public class MenuBar extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color TEXT          = Palette.withAlpha(Palette.getActive().getLight(), 170);
    private static final Color TEXT_SELECTED = Palette.getActive().getLight();
    private static final Color UNDERLINE     = Palette.getActive().getAccent2();
    private static final Color FILL_HOVER    = Palette.withAlpha(Palette.getActive().getLight(), 26);

    private static final int   ITEM_HEIGHT      = 32;
    private static final int   SIDE_PADDING     = 14;
    private static final int   UNDERLINE_HEIGHT = 2;
    private static final float LABEL_SIZE       = 12f;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<Item> items = new ArrayList<>();

    private int selected;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds an empty menu, ready for a caller to add items.
     */
    public MenuBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /**
     * Returns the position of the selected item, counting from 0 in the order that the items were added.
     */
    public int getSelectedIndex() {
        return selected;
    }

    public int getItemCount() {
        return items.size();
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Appends an item to the right of every item already added.
     *
     * @param name   the text to draw
     * @param action the action to run each time the item is selected
     * @throws IllegalArgumentException if the name or the action is null
     */
    public void addItem(String name, Runnable action) {
        if (name == null) {
            throw new IllegalArgumentException("A menu item requires a name");
        }

        if (action == null) {
            throw new IllegalArgumentException("A menu item requires an action");
        }

        var index = items.size();
        var item  = new Item(name, action);

        item.addActionListener(_ -> selectItem(index));
        item.setSelected(index == selected);

        items.add(item);
        add(item);
        revalidate();
    }

    /**
     * Selects an item, redraws every item to match, and runs the selected item's action, including where that
     * item was already selected.
     *
     * @param index the position of the item, counting from 0 in the order that the items were added
     * @throws IndexOutOfBoundsException if the index addresses no item
     */
    public void selectItem(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException(String.format("A menu of %d items has no item %d", items.size(), index));
        }

        selected = index;

        for (var position = 0; position < items.size(); position++) {
            items.get(position).setSelected(position == index);
        }

        items.get(index).runAction();
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * One entry on the menu, painted as its text over a hover fill, with an underline while it is selected.
     */
    private static final class Item extends JButton {

        private final Runnable action;

        Item(String name, Runnable action) {
            this.action = action;

            setText(name);
            setFont(getFont().deriveFont(Font.BOLD, LABEL_SIZE));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        void runAction() {
            action.run();
        }

        /**
         * Returns a width that fits the text plus {@value MenuBar#SIDE_PADDING} pixels either side, at a height
         * of {@value MenuBar#ITEM_HEIGHT} pixels.
         */
        @Override
        public Dimension getPreferredSize() {
            var metrics = getFontMetrics(getFont());

            return new Dimension(metrics.stringWidth(getText()) + SIDE_PADDING * 2, ITEM_HEIGHT);
        }

        /**
         * Paints the hover fill while the pointer is over the item, then the text, then the underline of a
         * selected item.
         */
        @Override
        protected void paintComponent(Graphics g) {
            var canvas = (Graphics2D) g.create();
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (getModel().isRollover()) {
                canvas.setColor(FILL_HOVER);
                canvas.fillRect(0, 0, getWidth(), getHeight());
            }

            var metrics  = canvas.getFontMetrics(getFont());
            var left     = (getWidth() - metrics.stringWidth(getText())) / 2;
            var baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();

            canvas.setFont(getFont());
            canvas.setColor(isSelected() ? TEXT_SELECTED : TEXT);
            canvas.drawString(getText(), left, baseline);

            if (isSelected()) {
                canvas.setColor(UNDERLINE);
                canvas.fillRect(SIDE_PADDING, getHeight() - UNDERLINE_HEIGHT, getWidth() - SIDE_PADDING * 2, UNDERLINE_HEIGHT);
            }

            canvas.dispose();
        }
    }

}
