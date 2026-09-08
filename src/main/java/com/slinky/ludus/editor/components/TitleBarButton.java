package com.slinky.ludus.editor.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;

/**
 * A caption button that paints one window control glyph at rest. A rectangle fills the whole button while the
 * pointer is over it and darkens while the button is held down, in grey for {@link Glyph#MINIMISE},
 * {@link Glyph#MAXIMISE} and {@link Glyph#RESTORE}, and in red under a white glyph for {@link Glyph#CLOSE}.
 * <p>
 * Painting is the only override, so an action listener added to a caption button behaves as it does on any
 * other {@link JButton}.
 * <p>
 * A single button covers both halves of the maximise toggle. A caller that maximises a window calls
 * {@link #setGlyph(Glyph)} with {@link Glyph#RESTORE}, and passes {@link Glyph#MAXIMISE} when it returns the
 * window to its previous size.
 * <p>
 * <b>Closing a window from a caption button</b>
 * <p>
 * A caller wires the close button to the window's own closing event:
 * <pre>{@code
 * var close = new TitleBarButton(TitleBarButton.Glyph.CLOSE);
 *
 * close.addActionListener(_ -> window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)));
 *
 * // the button reports a preferred size of 46 by 32 pixels
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class TitleBarButton extends JButton {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final int DEFAULT_WIDTH  = 46;
    private static final int DEFAULT_HEIGHT = 32;

    private static final Color GLYPH           = new Color(58, 64, 72);
    private static final Color GLYPH_DISABLED  = new Color(58, 64, 72, 70);
    private static final Color GLYPH_ON_CLOSE  = Color.WHITE;
    private static final Color FILL_HOVER      = new Color(0, 0, 0, 26);
    private static final Color FILL_PRESSED    = new Color(0, 0, 0, 54);
    private static final Color CLOSE_HOVER     = new Color(232, 17, 35);
    private static final Color CLOSE_PRESSED   = new Color(190, 20, 34);

    private static final float GLYPH_RATIO  = 0.32f;
    private static final float RESTORE_STEP = 0.28f;
    private static final float STROKE_WIDTH = 1f;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private Glyph glyph;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a caption button of the default size.
     *
     * @param glyph the window control that this button draws
     * @throws IllegalArgumentException if the glyph is null
     */
    public TitleBarButton(Glyph glyph) {
        this(glyph, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Builds a caption button of the given size. The glyph and the stroke that draws it scale with the height, so
     * a taller button keeps the same proportions.
     *
     * @param glyph  the window control that this button draws
     * @param width  the button width in pixels
     * @param height the button height in pixels, which also sets the glyph size
     * @throws IllegalArgumentException if the glyph is null, or either dimension is zero or negative
     */
    public TitleBarButton(Glyph glyph, int width, int height) {
        if (glyph == null) {
            throw new IllegalArgumentException("A title bar button requires a glyph");
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(String.format("Width and height must be positive, given %d by %d", width, height));
        }

        this.glyph = glyph;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setPreferredSize(new Dimension(width, height));
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public Glyph getGlyph() {
        return glyph;
    }

    // ========================================================================================== \\
    //                                          Setters                                           \\
    // ========================================================================================== \\
    /**
     * Replaces the glyph and repaints, which is how the maximise button becomes the restore button.
     *
     * @param glyph the window control that this button draws from now on
     * @throws IllegalArgumentException if the glyph is null
     */
    public void setGlyph(Glyph glyph) {
        if (glyph == null) {
            throw new IllegalArgumentException("A title bar button requires a glyph");
        }

        this.glyph = glyph;
        repaint();
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Paints the fill and the glyph, with the glyph centred in the button's bounds so that a layout giving the
     * button more room than it asked for leaves the artwork at the size that the constructor set.
     */
    @Override
    protected void paintComponent(Graphics g) {
        var canvas = (Graphics2D) g.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        paintFill(canvas);
        paintGlyph(canvas);

        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Fills the button's bounds while the pointer is over it, and leaves the background bare the rest of the
     * time. The close button fills red, which is the one control that a user benefits from picking out by
     * colour.
     */
    private void paintFill(Graphics2D canvas) {
        var model   = getModel();
        var pressed = model.isPressed() && model.isArmed();
        var closing = glyph == Glyph.CLOSE;

        if (pressed) {
            canvas.setColor(closing ? CLOSE_PRESSED : FILL_PRESSED);
        } else if (model.isRollover()) {
            canvas.setColor(closing ? CLOSE_HOVER : FILL_HOVER);
        } else {
            return;
        }

        canvas.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draws the glyph inside a square box at the centre of the button. Each coordinate falls on a half pixel, so
     * a one pixel stroke covers a single row or column of pixels.
     */
    private void paintGlyph(Graphics2D canvas) {
        var size = Math.round(getHeight() * GLYPH_RATIO);
        var left = Math.round((getWidth()  - size) / 2f) + 0.5f;
        var top  = Math.round((getHeight() - size) / 2f) + 0.5f;

        canvas.setColor(readGlyphColour());
        canvas.setStroke(new BasicStroke(STROKE_WIDTH));

        switch (glyph) {
            case MINIMISE -> canvas.draw(new Line2D.Float(left, top + size / 2f, left + size, top + size / 2f));
            case MAXIMISE -> canvas.draw(new Rectangle2D.Float(left, top, size, size));
            case RESTORE  -> paintRestore(canvas, left, top, size);
            case CLOSE    -> paintCross(canvas, left, top, size);
        }
    }

    /**
     * Draws two overlapping squares, the front one down and left of the back one. The back square appears as its
     * top and right edges alone, since the front square covers the other two.
     */
    private void paintRestore(Graphics2D canvas, float left, float top, int size) {
        var step  = Math.max(2f, size * RESTORE_STEP);
        var right = left + size;

        canvas.draw(new Line2D.Float(left + step, top, right, top));
        canvas.draw(new Line2D.Float(right, top, right, top + size - step));
        canvas.draw(new Rectangle2D.Float(left, top + step, size - step, size - step));
    }

    /**
     * Draws the two diagonals of the glyph box, which meet at its centre.
     */
    private void paintCross(Graphics2D canvas, float left, float top, int size) {
        canvas.draw(new Line2D.Float(left, top, left + size, top + size));
        canvas.draw(new Line2D.Float(left + size, top, left, top + size));
    }

    /**
     * Returns the colour that the glyph draws in, which turns white over the red fill that the close button
     * paints under the pointer.
     */
    private Color readGlyphColour() {
        var model = getModel();

        if (glyph == Glyph.CLOSE && (model.isRollover() || model.isPressed())) {
            return GLYPH_ON_CLOSE;
        }

        return isEnabled() ? GLYPH : GLYPH_DISABLED;
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * The window control that a caption button draws.
     */
    public enum Glyph {
        /** A horizontal bar, for the button that sends a window to the taskbar. */
        MINIMISE,

        /** A square, for the button that grows a window to fill the screen. */
        MAXIMISE,

        /** Two overlapping squares, for the button that returns a maximised window to its previous size. */
        RESTORE,

        /** Two crossed diagonals, for the button that closes a window. */
        CLOSE
    }

}
