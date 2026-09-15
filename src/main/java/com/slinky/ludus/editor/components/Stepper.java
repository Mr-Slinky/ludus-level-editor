package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.ui.SmallButton;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A pair of {@link SmallButton} arrows either side of a caption, which together choose one whole number out of a
 * range. The down arrow steps towards the minimum and the up arrow steps towards the maximum. A button at the
 * end of its travel is disabled and draws its arrow in grey, so the caption and the two buttons state both the
 * current value and the room left in either direction.
 * <p>
 * This control fills and outlines a capsule around all three, which groups them as one thing.
 * The caption divides in two: the name draws in a muted tone, and the value draws beside it in the accent that
 * a palette gives to a chosen value, so the number stands out from the name beside it.
 * <p>
 * The capsule lightens and its outline takes an accent while the pointer stands anywhere over the control,
 * either button included, so the whole group answers a hover rather than the one part under the pointer.
 * <p>
 * Two methods change the value, and the difference between them matters. {@link #setValue(int)} reports the new
 * value to every {@link ValueListener} registered through {@link #addValueListener(ValueListener)}, which is
 * what the two buttons call. {@link #showValue(int)} writes the caption alone, so a container that rejects a
 * change returns the control to the value that stands without hearing its own rejection back.
 * <p>
 * <b>Choosing the layer that a canvas stamps onto</b>
 * <p>
 * A caller builds a stepper over the layer range, wires it to a canvas, and steps it once:
 * <pre>{@code
 * var layer = new Stepper("Layer", 0, 5, 0);
 *
 * layer.addValueListener(canvas::setActiveLayer);
 * layer.increase();
 *
 * layer.getValue();    // 1, and the caption reads "Layer" beside "1"
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 2.0.0
 *          <p>
 *          Last modified: 2026-09-12
 * @since 1.0.0
 */
public class Stepper extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    // the control bar behind this control fills with the palette's chrome, so the capsule takes the darker
    // tone and appears sunk into the bar rather than raised off it
    private static final Color WELL          = Palette.getActive().getDark();
    private static final Color WELL_HOVER    = Palette.blend(Palette.getActive().getDark(), Palette.getActive().getAccent3(), 0.14f);
    private static final Color OUTLINE       = Palette.withAlpha(Palette.getActive().getLight(), 38);
    private static final Color OUTLINE_HOVER = Palette.withAlpha(Palette.getActive().getAccent3(), 210);
    private static final Color NAME_COLOUR   = Palette.withAlpha(Palette.getActive().getLight(), 170);
    private static final Color VALUE_COLOUR  = Palette.getActive().getAccent2();

    private static final int NAME_WIDTH    = 54;
    private static final int VALUE_WIDTH   = 24;
    private static final int GROUP_PADDING = 3;
    private static final int NAME_SIZE     = 11;
    private static final int VALUE_SIZE    = 13;

    private static final double POINT_UP   = Math.PI / 2;
    private static final double POINT_DOWN = 3 * Math.PI / 2;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<ValueListener> listeners = new ArrayList<>();

    private final SmallButton down   = SmallButton.blueRound(SmallButton.Symbol.LEFT_ARROW, POINT_DOWN);
    private final SmallButton up     = SmallButton.blueRound(SmallButton.Symbol.LEFT_ARROW, POINT_UP);
    private final JLabel      title  = new JLabel("", SwingConstants.RIGHT);
    private final JLabel      digits = new JLabel("", SwingConstants.CENTER);

    private final String name;
    private final int    minimum;
    private final int    maximum;

    private int     value;
    private boolean hovered;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a stepper over a range, starting at the given value.
     *
     * @param name    the word that the caption puts before the value
     * @param minimum the lowest value that the down button reaches
     * @param maximum the highest value that the up button reaches
     * @param value   the value to start on
     * @throws IllegalArgumentException if the name is null, the maximum falls below the minimum, or the starting
     *                                  value falls outside the range
     */
    public Stepper(String name, int minimum, int maximum, int value) {
        if (name == null) {
            throw new IllegalArgumentException("A stepper requires a name");
        }

        if (maximum < minimum) {
            throw new IllegalArgumentException(String.format("A maximum of %d falls below the minimum of %d", maximum, minimum));
        }

        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(String.format("A starting value of %d falls outside %d to %d", value, minimum, maximum));
        }

        this.name    = name;
        this.minimum = minimum;
        this.maximum = maximum;

        down.addActionListener(_ -> decrease());
        up.addActionListener(_ -> increase());

        styleLabel(title,  NAME_COLOUR,  Font.PLAIN, NAME_SIZE,  NAME_WIDTH);
        styleLabel(digits, VALUE_COLOUR, Font.BOLD,  VALUE_SIZE, VALUE_WIDTH);

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(GROUP_PADDING, GROUP_PADDING, GROUP_PADDING, GROUP_PADDING));
        add(down);
        add(title);
        add(digits);
        add(up);

        trackHover();
        showValue(value);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public int getValue() {
        return value;
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    // ========================================================================================== \\
    //                                          Setters                                           \\
    // ========================================================================================== \\
    /**
     * Moves to a value and reports it to every listener.
     *
     * @param value the value to move to
     * @throws IllegalArgumentException if the value falls outside the range
     */
    public void setValue(int value) {
        showValue(value);
        listeners.forEach(listener -> listener.handleValueChange(value));
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Writes a value to the caption and enables each button according to the room left in its direction, leaving
     * listeners unnotified. A container that turns a change down calls this to return the control to the value
     * that stands.
     *
     * @param value the value to display
     * @throws IllegalArgumentException if the value falls outside the range
     */
    public void showValue(int value) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(String.format("A value of %d falls outside %d to %d", value, minimum, maximum));
        }

        this.value = value;

        title.setText(name);
        digits.setText(String.valueOf(value));
        down.setEnabled(value > minimum);
        up.setEnabled(value < maximum);
    }

    /**
     * Steps one towards the maximum, and stays put on the maximum.
     */
    public void increase() {
        if (value < maximum) {
            setValue(value + 1);
        }
    }

    /**
     * Steps one towards the minimum, and stays put on the minimum.
     */
    public void decrease() {
        if (value > minimum) {
            setValue(value - 1);
        }
    }

    /**
     * Registers a listener that receives the value every time {@link #setValue(int)} changes it.
     *
     * @param listener the listener to notify
     */
    public void addValueListener(ValueListener listener) {
        listeners.add(listener);
    }

    /**
     * Fills the capsule that groups the two buttons with the caption, then leaves the children to paint over it.
     */
    @Override
    protected void paintComponent(Graphics g) {
        var canvas = (Graphics2D) g.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // a corner radius of the full height rounds each end into a half circle, which is what makes a capsule
        var arc    = getHeight();
        var right  = getWidth()  - 1;
        var bottom = getHeight() - 1;

        canvas.setColor(hovered ? WELL_HOVER : WELL);
        canvas.fillRoundRect(0, 0, right, bottom, arc, arc);

        canvas.setColor(hovered ? OUTLINE_HOVER : OUTLINE);
        canvas.drawRoundRect(0, 0, right, bottom, arc, arc);

        canvas.dispose();

        super.paintComponent(g);
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private void styleLabel(JLabel label, Color colour, int weight, int size, int width) {
        label.setForeground(colour);
        label.setFont(label.getFont().deriveFont(weight, (float) size));
        label.setPreferredSize(new Dimension(width, down.getPreferredSize().height));
    }

    /**
     * Lights the capsule while the pointer stands over any part of this control. Swing delivers a crossing to
     * the deepest component under the pointer alone, so every child reports its own, and this panel asks where
     * the pointer actually is rather than trusting the direction of the crossing.
     */
    private void trackHover() {
        var crossing = new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(getMousePosition(true) != null);
            }
        };

        addMouseListener(crossing);
        down.addMouseListener(crossing);
        up.addMouseListener(crossing);
        title.addMouseListener(crossing);
        digits.addMouseListener(crossing);
    }

    private void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            repaint();
        }
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * Receives the value that a stepper moves to.
     */
    @FunctionalInterface
    public interface ValueListener {

        /**
         * Handles a change of value.
         *
         * @param value the value now chosen
         */
        void handleValueChange(int value);
    }

}
