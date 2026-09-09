package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

/**
 * A capsule shaped button that paints its own fill, its own glow and its own label, so the look and feel that
 * the platform supplies reaches none of it.
 * <p>
 * The fill takes the accent given to the constructor. A halo of that same accent spreads outside the capsule
 * while the pointer is over the button, which lights the control without moving it, and the fill deepens while
 * the button is held down. The label draws in the palette's dark tone, since every accent here is bright enough
 * to carry dark text.
 * <p>
 * The button reports a preferred size wide enough for its text plus {@value #SIDE_PADDING} pixels either side,
 * so a caller adds one to a layout without measuring anything.
 * <p>
 * <b>Putting a save action in a control bar</b>
 * <pre>{@code
 * var save = new ActionButton("Save", Palette.getActive().getAccent3());
 *
 * save.addActionListener(_ -> writeTheLevel());
 * controlBar.addTrailing(save);
 *
 * // the button reports a height of 30 pixels, and a width that follows its text
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public class ActionButton extends JButton {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The distance in pixels between the text and either end of the capsule. */
    public static final int SIDE_PADDING = 20;

    /** The height of the capsule in pixels, which also sets its corner radius. */
    public static final int HEIGHT = 30;

    private static final Color LABEL = Palette.getActive().getDark();

    /** The room the halo needs outside the capsule, which the button reserves at each edge. */
    private static final int HALO_SPREAD = 3;

    private static final int   HALO_ALPHA    = 70;
    private static final float PRESSED_SHADE = 0.22f;
    private static final float LABEL_SIZE    = 12f;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final Color fill;
    private final Color fillPressed;
    private final Color halo;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a button that fills in the given accent.
     *
     * @param text   the label to draw
     * @param accent the colour that the capsule fills with
     * @throws IllegalArgumentException if the text or the accent is null
     */
    public ActionButton(String text, Color accent) {
        if (text == null) {
            throw new IllegalArgumentException("An action button requires a label");
        }

        if (accent == null) {
            throw new IllegalArgumentException("An action button requires an accent");
        }

        this.fill        = accent;
        this.fillPressed = Palette.blend(accent, Palette.getActive().getDark(), PRESSED_SHADE);
        this.halo        = Palette.withAlpha(accent, HALO_ALPHA);

        setText(text);
        setFont(getFont().deriveFont(Font.BOLD, LABEL_SIZE));
        setForeground(LABEL);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns a size holding the label between its side padding, at the fixed capsule height, with the halo's
     * spread added at every edge.
     *
     * @return the preferred size in pixels
     */
    @Override
    public Dimension getPreferredSize() {
        var metrics = getFontMetrics(getFont());
        var width   = metrics.stringWidth(getText()) + SIDE_PADDING * 2;

        return new Dimension(width + HALO_SPREAD * 2, HEIGHT + HALO_SPREAD * 2);
    }

    /**
     * Paints the halo, then the capsule, then the label. Swing draws the label itself for an ordinary button,
     * so this draws it here and leaves the superclass alone.
     */
    @Override
    protected void paintComponent(Graphics g) {
        var canvas = (Graphics2D) g.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintHalo(canvas);
        paintCapsule(canvas);
        paintLabel(canvas);

        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Draws a soft ring of the accent outside the capsule while the pointer is over the button. Each pass draws
     * one pixel further out at the same low opacity, which stacks into a glow that fades with distance.
     */
    private void paintHalo(Graphics2D canvas) {
        if (!getModel().isRollover()) {
            return;
        }

        canvas.setColor(halo);

        for (var spread = HALO_SPREAD; spread > 0; spread--) {
            var arc = HEIGHT + spread * 2;

            canvas.drawRoundRect(
                    HALO_SPREAD - spread,
                    HALO_SPREAD - spread,
                    getWidth()  - 1 - (HALO_SPREAD - spread) * 2,
                    getHeight() - 1 - (HALO_SPREAD - spread) * 2,
                    arc, arc
            );
        }
    }

    private void paintCapsule(Graphics2D canvas) {
        var model   = getModel();
        var pressed = model.isPressed() && model.isArmed();

        canvas.setColor(pressed ? fillPressed : fill);
        canvas.fillRoundRect(HALO_SPREAD, HALO_SPREAD, getWidth() - HALO_SPREAD * 2, HEIGHT, HEIGHT, HEIGHT);
    }

    /**
     * Draws the label centred in the capsule, sitting it on the font's baseline so that the text centres on its
     * own height rather than on the space that the font reserves above and below it.
     */
    private void paintLabel(Graphics2D canvas) {
        var metrics  = canvas.getFontMetrics(getFont());
        var left     = (getWidth() - metrics.stringWidth(getText())) / 2;
        var baseline = HALO_SPREAD + (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

        canvas.setFont(getFont());
        canvas.setColor(getForeground());
        canvas.drawString(getText(), left, baseline);
    }

}
