package com.slinky.ludus.editor.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import javax.swing.JButton;

/**
 * A round navigation button that paints a single chevron and nothing else at rest. A circle fades in behind the
 * chevron while the pointer is over the button, and darkens while the button is held down.
 * <p>
 * The class extends {@link JButton} and overrides {@link #paintComponent(Graphics)} alone, so the button model
 * continues to supply the rollover, armed and pressed states the painting reads, and
 * {@link JButton#addActionListener(java.awt.event.ActionListener)} behaves as it does on any other button.
 * <p>
 * <b>Stepping through a deck</b>
 * <pre>{@code
 * var previous = new ChevronButton(ChevronButton.Direction.LEFT);
 * var next     = new ChevronButton(ChevronButton.Direction.RIGHT);
 *
 * previous.addActionListener(e -> deck.showPrevious());
 * next.addActionListener(e -> deck.showNext());
 *
 * // both report a preferred size of 28 by 28 pixels
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-06
 * @since 1.0.0
 */
public class ChevronButton extends JButton {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final int DEFAULT_DIAMETER = 28;

    private static final Color CHEVRON          = new Color(58, 64, 72);
    private static final Color CHEVRON_DISABLED = new Color(58, 64, 72, 70);
    private static final Color HALO_HOVER       = new Color(0, 0, 0, 26);
    private static final Color HALO_PRESSED     = new Color(0, 0, 0, 54);

    private static final float CHEVRON_WIDTH_RATIO  = 0.24f;
    private static final float CHEVRON_HEIGHT_RATIO = 0.42f;
    private static final float STROKE_RATIO         = 0.085f;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final Direction direction;
    private final int       diameter;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a chevron button of the default diameter.
     *
     * @param direction the way the chevron points
     */
    public ChevronButton(Direction direction) {
        this(direction, DEFAULT_DIAMETER);
    }

    /**
     * Builds a chevron button of the given diameter. The chevron and the stroke that draws it scale with the
     * diameter, so a larger button keeps the same proportions.
     *
     * @param direction the way the chevron points
     * @param diameter  the width and height of the halo in pixels
     * @throws IllegalArgumentException if the direction is null or the diameter is zero or negative
     */
    public ChevronButton(Direction direction, int diameter) {
        if (direction == null) {
            throw new IllegalArgumentException("A chevron button requires a direction");
        }

        if (diameter <= 0) {
            throw new IllegalArgumentException(String.format("Diameter must be positive, given %d", diameter));
        }

        this.direction = direction;
        this.diameter  = diameter;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(diameter, diameter));
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public Direction getDirection() {
        return direction;
    }

    public int getDiameter() {
        return diameter;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Paints the halo and the chevron, centred in the button's bounds so that a layout giving the button more
     * room than it asked for leaves the artwork the size the constructor set.
     */
    @Override
    protected void paintComponent(Graphics g) {
        var canvas = (Graphics2D) g.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        var centreX = getWidth()  / 2f;
        var centreY = getHeight() / 2f;

        paintHalo(canvas, centreX, centreY);
        paintChevron(canvas, centreX, centreY);

        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Fills the circle behind the chevron while the pointer is over the button, and leaves the background bare
     * the rest of the time.
     */
    private void paintHalo(Graphics2D canvas, float centreX, float centreY) {
        var model = getModel();

        if (model.isPressed() && model.isArmed()) {
            canvas.setColor(HALO_PRESSED);
        } else if (model.isRollover()) {
            canvas.setColor(HALO_HOVER);
        } else {
            return;
        }

        var radius = diameter / 2f;
        canvas.fill(new Ellipse2D.Float(centreX - radius, centreY - radius, diameter, diameter));
    }

    /**
     * Draws the chevron as two strokes meeting at the tip, with round caps and a round join so the corner reads
     * as a single soft angle.
     */
    private void paintChevron(Graphics2D canvas, float centreX, float centreY) {
        var halfWidth  = diameter * CHEVRON_WIDTH_RATIO  / 2f;
        var halfHeight = diameter * CHEVRON_HEIGHT_RATIO / 2f;
        var pointsLeft = direction == Direction.LEFT;
        var tipX       = pointsLeft ? centreX - halfWidth : centreX + halfWidth;
        var tailX      = pointsLeft ? centreX + halfWidth : centreX - halfWidth;

        var chevron = new GeneralPath();
        chevron.moveTo(tailX, centreY - halfHeight);
        chevron.lineTo(tipX, centreY);
        chevron.lineTo(tailX, centreY + halfHeight);

        canvas.setColor(isEnabled() ? CHEVRON : CHEVRON_DISABLED);
        canvas.setStroke(new BasicStroke(diameter * STROKE_RATIO, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.draw(chevron);
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * The way a chevron points, which is also the direction the button steps in.
     */
    public enum Direction {
        LEFT,
        RIGHT
    }

}
