package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.UncheckedIOException;

import javax.swing.JButton;

/**
 * A button that paints itself from a nine slice sprite sheet, so one set of artwork draws at every size that a
 * layout gives it.
 * <p>
 * A {@link Skin} defines the two sheets that a button draws from, one for the button at rest and one for the
 * button while a user holds it down. Each sheet stores nine pieces on a square grid: four corners, four edges
 * and one centre. The button draws each corner at its own size in the matching corner of its bounds, stretches
 * the top and bottom edges across the width between them, stretches the left and right edges down the height,
 * and stretches the centre across what remains. Interpolation is set to nearest neighbour, so the pixels stay
 * square wherever a piece stretches.
 * <p>
 * The smallest size that a skin composes at is twice its cell size in each direction, which is the point at
 * which the four corners meet. {@link #getPreferredSize()} returns that size, widened to fit the label together
 * with {@value #SIDE_PADDING} pixels either side.
 * <p>
 * The label draws centred, and it moves down by {@link Skin#getPressedDrop()} pixels while a user holds the
 * button down, which keeps it on the face of the pressed artwork.
 * <p>
 * <b>Putting a save action on a panel</b>
 * <pre>{@code
 * var save = new SpriteButton("Save", SpriteButton.Skin.BIG_BLUE);
 *
 * save.addActionListener(_ -> writeTheLevel());
 * panel.add(save);
 *
 * // the button reports a preferred size of 128 by 128 pixels
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-10
 * @since 1.0.0
 */
public class SpriteButton extends JButton {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The room in pixels that {@link #getPreferredSize()} leaves between the label and either end. */
    public static final int SIDE_PADDING = 24;

    private static final Color LABEL          = Palette.getActive().getLight();
    private static final Color LABEL_DISABLED = Palette.withAlpha(Palette.getActive().getLight(), 70);

    /** The number of pieces across one sheet, which equals the number down it. */
    private static final int SLICES = 3;

    private static final float LABEL_SIZE = 14f;

    // ========================================================================================== \\
    //                                       Nested Classes                                       \\
    // ========================================================================================== \\
    /**
     * One pair of nine slice sheets, together with the grid that their pieces are laid out on.
     * <p>
     * {@link #getCellSize()} returns the width and height of a single piece, and {@link #getCellStride()}
     * returns the distance from the left edge of one piece to the left edge of the next, which exceeds the cell
     * size wherever the artist left space between them.
     * <p>
     * Both sheets draw the same button, and the pressed one draws its artwork lower within its own cells.
     * {@link #getPressedDrop()} returns that distance in pixels, which a caller adds to the position of
     * whatever it draws over the face.
     * <p>
     * Each constant reads a sheet from the classpath on the first call that needs it and keeps the image it
     * read, so the artwork decodes once however many buttons share the skin.
     */
    public enum Skin {

        /** The large teal button. Every piece measures 64 pixels square, and the pieces start every 128 pixels. */
        BIG_BLUE(
                "/ui-elements/buttons/bigbluebutton_regular.png",
                "/ui-elements/buttons/bigbluebutton_pressed.png",
                64, 128, 6
        );

        private final String regularPath;
        private final String pressedPath;
        private final int    cellSize;
        private final int    cellStride;
        private final int    pressedDrop;

        private BufferedImage regular;
        private BufferedImage pressed;

        Skin(String regularPath, String pressedPath, int cellSize, int cellStride, int pressedDrop) {
            this.regularPath = regularPath;
            this.pressedPath = pressedPath;
            this.cellSize    = cellSize;
            this.cellStride  = cellStride;
            this.pressedDrop = pressedDrop;
        }

        /** Returns the width and height in pixels of a single piece. */
        public int getCellSize() {
            return cellSize;
        }

        /** Returns the distance in pixels from one piece to the next across the sheet. */
        public int getCellStride() {
            return cellStride;
        }

        /** Returns the distance in pixels that the pressed artwork moves down within its own cells. */
        public int getPressedDrop() {
            return pressedDrop;
        }

        /**
         * Returns the sheet that draws the button at rest, reading it from the classpath on the first call.
         *
         * @return the decoded sheet
         * @throws IllegalArgumentException if the classpath contains no resource at that path, or the resource
         *                                  decodes to no image
         * @throws UncheckedIOException     if reading the resource fails
         */
        public BufferedImage readRegularSheet() {
            if (regular == null) {
                regular = Swatch.loadImage(regularPath);
            }

            return regular;
        }

        /**
         * Returns the sheet that draws the button while a user holds it down, reading it from the classpath on
         * the first call.
         *
         * @return the decoded sheet
         * @throws IllegalArgumentException if the classpath contains no resource at that path, or the resource
         *                                  decodes to no image
         * @throws UncheckedIOException     if reading the resource fails
         */
        public BufferedImage readPressedSheet() {
            if (pressed == null) {
                pressed = Swatch.loadImage(pressedPath);
            }

            return pressed;
        }

    }

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final Skin skin;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a button that draws from the given skin.
     *
     * @param text the label to draw
     * @param skin the pair of sheets that the button draws from
     * @throws IllegalArgumentException if the text or the skin is null
     */
    public SpriteButton(String text, Skin skin) {
        if (text == null) {
            throw new IllegalArgumentException("A sprite button requires a label");
        }

        if (skin == null) {
            throw new IllegalArgumentException("A sprite button requires a skin");
        }

        this.skin = skin;

        setText(text);
        setFont(getFont().deriveFont(Font.BOLD, LABEL_SIZE));
        setForeground(LABEL);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /** Returns the pair of sheets that this button draws from. */
    public Skin getSkin() {
        return skin;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns a size twice the skin's cell size in each direction, which is the smallest size that the nine
     * pieces compose at. The width grows where the label together with {@value #SIDE_PADDING} pixels either
     * side asks for more than that.
     *
     * @return the preferred size in pixels
     */
    @Override
    public Dimension getPreferredSize() {
        var metrics = getFontMetrics(getFont());
        var minimum = skin.getCellSize() * 2;
        var width   = Math.max(minimum, metrics.stringWidth(getText()) + SIDE_PADDING * 2);

        return new Dimension(width, minimum);
    }

    /**
     * Paints the nine pieces of the current sheet, then the label over them. Swing draws the label itself for an
     * ordinary button, so this draws it here and leaves the superclass alone.
     */
    @Override
    protected void paintComponent(Graphics g) {
        var model  = getModel();
        var held   = model.isPressed() && model.isArmed();
        var canvas = (Graphics2D) g.create();

        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintSheet(canvas, held ? skin.readPressedSheet() : skin.readRegularSheet());
        paintLabel(canvas, held ? skin.getPressedDrop() : 0);

        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Draws all nine pieces of one sheet into the button's bounds. The four arrays state where each column and
     * each row of the button starts and how far it runs, so the two loops below pair a piece of the sheet with
     * the rectangle that it fills.
     */
    private void paintSheet(Graphics2D canvas, BufferedImage sheet) {
        var cell   = skin.getCellSize();
        var stride = skin.getCellStride();

        var left = new int[] {0, cell, getWidth()  - cell};
        var top  = new int[] {0, cell, getHeight() - cell};
        var wide = new int[] {cell, Math.max(0, getWidth()  - cell * 2), cell};
        var tall = new int[] {cell, Math.max(0, getHeight() - cell * 2), cell};

        for (var row = 0; row < SLICES; row++) {
            for (var column = 0; column < SLICES; column++) {
                paintPiece(
                        canvas, sheet,
                        left[column], top[row], wide[column], tall[row],
                        column * stride, row * stride
                );
            }
        }
    }

    /**
     * Draws one cell of the sheet into a rectangle of the button. A button squeezed down to the width of its
     * own corners leaves the middle column zero pixels wide, and a rectangle that size is skipped.
     */
    private void paintPiece(Graphics2D canvas, BufferedImage sheet, int x, int y, int width, int height, int sourceX, int sourceY) {
        if (width <= 0 || height <= 0) {
            return;
        }

        var cell = skin.getCellSize();

        canvas.drawImage(sheet, x, y, x + width, y + height, sourceX, sourceY, sourceX + cell, sourceY + cell, null);
    }

    /**
     * Draws the label centred in the button, placing it on the font's baseline so that the text centres on its
     * own height rather than on the space that the font reserves above and below it. The drop moves the label
     * down with the pressed artwork.
     */
    private void paintLabel(Graphics2D canvas, int drop) {
        var metrics  = canvas.getFontMetrics(getFont());
        var left     = (getWidth()  - metrics.stringWidth(getText())) / 2;
        var baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() + drop;

        canvas.setFont(getFont());
        canvas.setColor(isEnabled() ? LABEL : LABEL_DISABLED);
        canvas.drawString(getText(), left, baseline);
    }

}
