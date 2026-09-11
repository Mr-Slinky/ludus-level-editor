package com.slinky.ludus.editor.components;

import java.awt.AlphaComposite;
import java.awt.Dimension;
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
 * An {@link Icon} from the same pack draws centred over that artwork, and it moves down by
 * {@link Skin#getPressedDrop()} pixels while a user holds the button down, which keeps it on the face of the
 * pressed sheet.
 * <p>
 * The smallest size that a skin composes at is twice its cell size in each direction, which is the point at
 * which the four corners meet. {@link #getPreferredSize()} returns that size, widened to fit the icon together
 * with {@value #SIDE_PADDING} pixels either side.
 * <p>
 * <b>Putting a save action on a panel</b>
 * <pre>{@code
 * var save = new SpriteButton(SpriteButton.Icon.SHIELD, SpriteButton.Skin.BIG_BLUE);
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
 *          Last modified: 2026-09-11
 * @since 1.0.0
 */
public class SpriteButton extends JButton {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The room in pixels that {@link #getPreferredSize()} leaves between the icon and either end. */
    public static final int SIDE_PADDING = 24;

    /** The opacity from 0 to 255 that the icon draws at while the button is disabled. */
    private static final int DISABLED_ALPHA = 70;

    /** The number of pieces across one sheet, which equals the number down it. */
    private static final int SLICES = 3;

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

    /**
     * One emblem from the interface pack, which a button draws on its face.
     * <p>
     * Every icon in the pack measures 64 pixels square, which fits inside the face of the smallest button that
     * any {@link Skin} composes.
     * <p>
     * Each constant reads its image from the classpath on the first call that needs it and keeps the image it
     * read, so the artwork decodes once however many buttons share the icon.
     */
    public enum Icon {

        /** A shield, quartered white over blue. */
        SHIELD("/ui-elements/icons/icon_06.png");

        private final String path;

        private BufferedImage image;

        Icon(String path) {
            this.path = path;
        }

        /**
         * Returns the artwork, reading it from the classpath on the first call.
         *
         * @return the decoded image
         * @throws IllegalArgumentException if the classpath contains no resource at that path, or the resource
         *                                  decodes to no image
         * @throws UncheckedIOException     if reading the resource fails
         */
        public BufferedImage readImage() {
            if (image == null) {
                image = Swatch.loadImage(path);
            }

            return image;
        }

    }

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final Skin skin;
    private final Icon icon;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a button that draws the given icon over the given artwork.
     *
     * @param icon the emblem to draw on the face
     * @param skin the pair of sheets that the button draws from
     * @throws IllegalArgumentException if the icon or the skin is null
     */
    public SpriteButton(Icon icon, Skin skin) {
        if (icon == null) {
            throw new IllegalArgumentException("A sprite button requires an icon");
        }

        if (skin == null) {
            throw new IllegalArgumentException("A sprite button requires a skin");
        }

        this.icon = icon;
        this.skin = skin;

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

    /** Returns the emblem that this button draws on its face. */
    public Icon getSpriteIcon() {
        return icon;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns a size twice the skin's cell size in each direction, which is the smallest size that the nine
     * pieces compose at. The width grows where the icon, together with {@value #SIDE_PADDING} pixels either
     * side, asks for more than that.
     *
     * @return the preferred size in pixels
     */
    @Override
    public Dimension getPreferredSize() {
        var minimum = skin.getCellSize() * 2;
        var face    = icon.readImage().getWidth() + SIDE_PADDING * 2;

        return new Dimension(Math.max(minimum, face), minimum);
    }

    /**
     * Paints the nine pieces of the current sheet, then the icon over them.
     */
    @Override
    protected void paintComponent(Graphics g) {
        var model  = getModel();
        var held   = model.isPressed() && model.isArmed();
        var canvas = (Graphics2D) g.create();

        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        paintSheet(canvas, held ? skin.readPressedSheet() : skin.readRegularSheet());
        paintIcon(canvas, held ? skin.getPressedDrop() : 0);

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
     * Draws the icon centred in the button at its own size, which every skin's face is wide enough and tall
     * enough to take. The drop moves the icon down with the pressed artwork, and a disabled button draws it at
     * an opacity of {@value #DISABLED_ALPHA} in 255.
     */
    private void paintIcon(Graphics2D canvas, int drop) {
        var art  = icon.readImage();
        var left = (getWidth()  - art.getWidth())  / 2;
        var top  = (getHeight() - art.getHeight()) / 2 + drop;

        if (!isEnabled()) {
            canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DISABLED_ALPHA / 255f));
        }

        canvas.drawImage(art, left, top, null);
    }

}
