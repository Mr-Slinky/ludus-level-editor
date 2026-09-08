package com.slinky.ludus.editor.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The strip along the bottom of the window, where a caller adds the controls that act on the level itself. Two
 * areas take components: {@link #addLeading(JComponent)} places one against the left edge, and
 * {@link #addTrailing(JComponent)} places one against the right.
 * <p>
 * A separator line runs along the top of the strip, and a margin surrounds both areas, so a control added to
 * either one stands clear of the window edge and of the canvas above.
 * <p>
 * <b>Putting a layer stepper along the bottom of a window</b>
 * <p>
 * A caller builds the strip, adds one control against its left edge, and drops it into a frame:
 * <pre>{@code
 * var bar = new ControlBar();
 * bar.addLeading(new Stepper("Layer", 0, 5, 0));
 *
 * frame.add(bar, BorderLayout.SOUTH);
 *
 * // the strip takes its height from the tallest control added to it
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class ControlBar extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color BACKGROUND = new Color(243, 243, 243);
    private static final Color SEPARATOR  = new Color(0, 0, 0, 30);

    private static final int PADDING = 6;
    private static final int GAP     = 6;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final JPanel leading  = new JPanel(new FlowLayout(FlowLayout.LEFT,  GAP, 0));
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0));

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds an empty strip, ready for a caller to add controls to either end.
     */
    public ControlBar() {
        leading.setOpaque(false);
        trailing.setOpaque(false);

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR),
                BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));
        add(leading, BorderLayout.WEST);
        add(trailing, BorderLayout.EAST);
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Adds a control against the left edge of the strip, to the right of anything already added there.
     *
     * @param component the control to add
     */
    public void addLeading(JComponent component) {
        leading.add(component);
        revalidate();
    }

    /**
     * Adds a control against the right edge of the strip, to the right of anything already added there.
     *
     * @param component the control to add
     */
    public void addTrailing(JComponent component) {
        trailing.add(component);
        revalidate();
    }

}
