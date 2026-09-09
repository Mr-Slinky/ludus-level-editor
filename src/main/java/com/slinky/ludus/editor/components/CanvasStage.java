package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * The surface that a {@link LevelCanvas} stands on, which centres the grid while the window has room for it and
 * scrolls to it once the grid outgrows the window.
 * <p>
 * A grid of 15 rows at 64 pixels a cell stands 960 pixels tall, which is taller than most windows leave for it.
 * A canvas states that size as its minimum as well as its preferred size, so a layout short of room clips it
 * rather than shrinking it. This stage gives that case somewhere to go: the grid keeps every pixel, and a user
 * reaches the part off screen by scrolling.
 * <p>
 * The two scrollbars appear only while they are needed, and they paint as a slim thumb with no arrow buttons in
 * the palette's own tones, so a scrollbar that does appear belongs to the window around it.
 * <p>
 * <b>Standing a canvas on a stage</b>
 * <pre>{@code
 * var canvas = new LevelCanvas(64, 15, 15);
 * var stage  = new CanvasStage(canvas, 64);
 *
 * body.add(stage, BorderLayout.CENTER);
 *
 * // a grid smaller than the stage centres in it, and a larger one scrolls one cell at a time
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-09
 * @since 1.0.0
 */
public class CanvasStage extends JScrollPane {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The width in pixels of either scrollbar. */
    public static final int BAR_WIDTH = 10;

    private static final Color SURFACE   = Palette.blend(Palette.getActive().getDark(), Palette.getActive().getLight(), 0.035f);
    private static final Color EDGE      = Palette.withAlpha(Palette.getActive().getLight(), 22);
    private static final Color THUMB     = Palette.withAlpha(Palette.getActive().getLight(), 55);
    private static final Color THUMB_LIT = Palette.getActive().getAccent2();

    private static final int THUMB_INSET = 2;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a stage over one component.
     *
     * @param content        the component to centre and scroll, which is a {@link LevelCanvas} in the editor
     * @param unitIncrement  the distance in pixels that one turn of a wheel scrolls, which is one cell
     * @throws IllegalArgumentException if the content is null, or if the increment is zero or negative
     */
    public CanvasStage(JComponent content, int unitIncrement) {
        if (content == null) {
            throw new IllegalArgumentException("A canvas stage requires a component to hold");
        }

        if (unitIncrement <= 0) {
            throw new IllegalArgumentException(String.format("A scroll increment must be positive, given %d", unitIncrement));
        }

        setViewportView(new Centre(content, unitIncrement));
        setBorder(BorderFactory.createLineBorder(EDGE));
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);

        getViewport().setBackground(SURFACE);
        getViewport().setOpaque(true);

        styleScrollBar(getVerticalScrollBar(),   unitIncrement);
        styleScrollBar(getHorizontalScrollBar(), unitIncrement);
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private static void styleScrollBar(JScrollBar bar, int unitIncrement) {
        bar.setUI(new SlimScrollBarUI());
        bar.setUnitIncrement(unitIncrement);
        bar.setPreferredSize(new Dimension(BAR_WIDTH, BAR_WIDTH));
        bar.setBackground(SURFACE);
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * The view inside the stage's viewport, which centres its one child and reports its own size back to the
     * scroll pane.
     * <p>
     * The two {@code tracksViewport} methods are what let one component both centre and scroll. Each returns
     * {@code true} while the viewport is the larger of the two, which stretches this panel to fill the viewport
     * so that its {@link GridBagLayout} centres the child in it. Each returns {@code false} once the child is
     * the larger, which leaves this panel at the child's own size and gives the scroll pane something to
     * scroll.
     */
    private static class Centre extends JPanel implements Scrollable {

        private final int unitIncrement;

        Centre(JComponent content, int unitIncrement) {
            this.unitIncrement = unitIncrement;

            setLayout(new GridBagLayout());
            setBackground(SURFACE);
            add(content);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return unitIncrement;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport viewport && viewport.getWidth() > getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }

    /**
     * A scrollbar drawn as a rounded thumb on a bare track, with the arrow buttons taken down to nothing. The
     * thumb takes the palette's hover accent while a user is over it or dragging it.
     */
    private static class SlimScrollBarUI extends BasicScrollBarUI {

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return buildHiddenButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return buildHiddenButton();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent component, Rectangle bounds) {
            g.setColor(SURFACE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent component, Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            var canvas = (Graphics2D) g.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            var width  = bounds.width  - THUMB_INSET * 2;
            var height = bounds.height - THUMB_INSET * 2;
            var arc    = Math.min(width, height);

            canvas.setColor(isThumbRollover() || isDragging() ? THUMB_LIT : THUMB);
            canvas.fillRoundRect(bounds.x + THUMB_INSET, bounds.y + THUMB_INSET, width, height, arc, arc);
            canvas.dispose();
        }

        /**
         * Reports whether a user is holding the thumb, which the superclass tracks but keeps to itself.
         */
        private boolean isDragging() {
            return scrollbar.getValueIsAdjusting();
        }

        /**
         * Returns a button of no size, which is how a scrollbar loses its arrows without losing its layout.
         */
        private static JButton buildHiddenButton() {
            var button = new JButton();

            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));

            return button;
        }
    }

}
