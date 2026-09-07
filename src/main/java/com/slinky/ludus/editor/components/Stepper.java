package com.slinky.ludus.editor.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A pair of chevron buttons either side of a caption, which together choose one whole number out of a range. The
 * down button steps towards the minimum and the up button steps towards the maximum, and each one turns grey at
 * the end of its travel, so the caption and the two buttons state both the current value and how much room is
 * left in either direction.
 * <p>
 * An outline surrounds all three, which groups them as one control. The caption reads the name given to the
 * constructor followed by the value, as in {@code Columns 10}.
 * <p>
 * Two methods change the value, and the difference between them matters. {@link #setValue(int)} reports the new
 * value to every {@link ValueListener} registered through {@link #addValueListener(ValueListener)}, which is
 * what the two buttons call. {@link #showValue(int)} writes the caption alone, so a container that rejects a
 * change returns the control to the value that stands without hearing its own rejection back.
 * <p>
 * <b>Choosing the layer a canvas stamps onto</b>
 * <pre>{@code
 * var layer = new Stepper("Layer", 0, 5, 0);
 *
 * layer.addValueListener(canvas::setActiveLayer);
 * layer.increase();
 *
 * layer.getValue();    // 1, and the caption reads "Layer 1"
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class Stepper extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color OUTLINE = new Color(0, 0, 0, 40);

    private static final int CAPTION_WIDTH = 84;
    private static final int GROUP_PADDING = 3;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<ValueListener> listeners = new ArrayList<>();

    private final ChevronButton down    = new ChevronButton(ChevronButton.Direction.DOWN);
    private final ChevronButton up      = new ChevronButton(ChevronButton.Direction.UP);
    private final JLabel        caption = new JLabel("", SwingConstants.CENTER);

    private final String name;
    private final int    minimum;
    private final int    maximum;

    private int value;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a stepper over a range, starting at the given value.
     *
     * @param name    the word the caption puts before the value
     * @param minimum the lowest value the down button reaches
     * @param maximum the highest value the up button reaches
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

        caption.setPreferredSize(new Dimension(CAPTION_WIDTH, down.getDiameter()));

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE, 1, true),
                BorderFactory.createEmptyBorder(GROUP_PADDING, GROUP_PADDING, GROUP_PADDING, GROUP_PADDING)));
        add(down);
        add(caption);
        add(up);

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

        caption.setText(String.format("%s %d", name, value));
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

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * Receives the value a stepper moves to.
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
