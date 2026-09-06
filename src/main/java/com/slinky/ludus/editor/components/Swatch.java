package com.slinky.ludus.editor.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Takes a single image and turns it into a swatch of selectable tiles, drawn at the image's natural size. The
 * tile width and height determine how many rows and columns this swatch has. An image of dimensions 192x64,
 * cut into 64 pixel tiles, yields one row (64 image height / 64 tile height = 1 row) and three columns
 * (192 image width / 64 tile width = 3 columns).
 * <p>
 * A press selects the tile under the pointer, and a drag extends the selection to a rectangle of tiles.
 * {@link #getSelection()} returns that rectangle in tile coordinates, where {@code x} is the leftmost column,
 * {@code y} is the topmost row, and the width and height count tiles rather than pixels.
 * <p>
 * <b>Selecting from a tileset</b>
 * <pre>{@code
 * var swatch = new Swatch("terrain/tilesets/Tilemap_color1.png", 64);
 * swatch.addSelectionListener(selection -> preview.showImage(swatch.readSelectedImage()));
 *
 * // a drag from the first tile of the first row to the third tile of the second row leaves
 * // getSelection() equal to java.awt.Rectangle[x=0,y=0,width=3,height=2]
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-06
 * @since 1.0.0
 */
public class Swatch extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    public static final String ROOT_DIR = "/assets";

    private static final Color GRID_COLOUR       = new Color(0, 0, 0, 90);
    private static final Color SELECTION_FILL    = new Color(255, 255, 255, 70);
    private static final Color SELECTION_OUTLINE = new Color(255, 214, 0);

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<SelectionListener> listeners = new ArrayList<>();
    private final BufferedImage image;
    private final int tileWidth;
    private final int tileHeight;
    private final int columns;
    private final int rows;

    private Rectangle selection = new Rectangle(0, 0, 1, 1);
    private Point     anchor    = new Point(0, 0);

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Loads an image from the assets directory and cuts it into square tiles.
     *
     * @param imagePath the path below {@value #ROOT_DIR}, with or without a leading slash
     * @param dimension the tile width and height in pixels
     */
    public Swatch(String imagePath, int dimension) {
        this(imagePath, dimension, dimension);
    }

    /**
     * Loads an image from the assets directory and cuts it into tiles of the given size.
     *
     * @param imagePath  the path below {@value #ROOT_DIR}, with or without a leading slash
     * @param tileWidth  the tile width in pixels
     * @param tileHeight the tile height in pixels
     */
    public Swatch(String imagePath, int tileWidth, int tileHeight) {
        this(loadImage(imagePath), tileWidth, tileHeight);
    }

    /**
     * Cuts an image already in memory into square tiles.
     *
     * @param image     the image to draw and divide
     * @param dimension the tile width and height in pixels
     */
    public Swatch(BufferedImage image, int dimension) {
        this(image, dimension, dimension);
    }

    /**
     * Cuts an image already in memory into tiles of the given size. An image whose width is not a whole number
     * of tiles leaves the remaining pixels drawn and outside every tile.
     *
     * @param image      the image to draw and divide
     * @param tileWidth  the tile width in pixels
     * @param tileHeight the tile height in pixels
     * @throws IllegalArgumentException if the image is null, if either tile dimension is zero or negative, or
     *                                  if the image is smaller than a single tile
     */
    public Swatch(BufferedImage image, int tileWidth, int tileHeight) {
        if (image == null) {
            throw new IllegalArgumentException("A swatch requires an image");
        }

        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException(String.format("Tile size must be positive, given %d by %d", tileWidth, tileHeight));
        }

        this.image      = image;
        this.tileWidth  = tileWidth;
        this.tileHeight = tileHeight;
        this.columns    = image.getWidth()  / tileWidth;
        this.rows       = image.getHeight() / tileHeight;

        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException(String.format("An image of %d by %d fits no tile of %d by %d", image.getWidth(), image.getHeight(), tileWidth, tileHeight));
        }

        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        installMouseHandling();
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    /**
     * Returns the selected tiles in tile coordinates, where {@code x} is the leftmost column, {@code y} is the
     * topmost row, and the width and height count tiles. A fresh swatch returns the single tile at the origin.
     *
     * @return a copy of the current selection, which spans at least one tile
     */
    public Rectangle getSelection() {
        return new Rectangle(selection);
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Registers a listener that receives the selection every time it changes.
     *
     * @param listener the listener to notify
     */
    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns the region of the source image the selection covers, at the image's own resolution. A selection of
     * three columns by two rows over 64 pixel tiles returns a 192 by 128 image.
     *
     * @return a view onto the source image, sharing its pixels
     */
    public BufferedImage readSelectedImage() {
        var bounds = resolveSelectionBounds();
        return image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        var canvas = (Graphics2D) g.create();
        canvas.drawImage(image, 0, 0, null);
        paintGrid(canvas);
        paintSelection(canvas);
        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private static BufferedImage loadImage(String path) {
        var resource = ROOT_DIR + normalisePath(path);

        try (var source = Swatch.class.getResourceAsStream(resource)) {
            if (source == null) {
                throw new IllegalArgumentException(String.format("The classpath contains no resource at '%s'", resource));
            }

            var loaded = ImageIO.read(source);
            if (loaded == null) {
                throw new IllegalArgumentException(String.format("The resource at '%s' decodes to no image", resource));
            }

            return loaded;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to read the image at '%s'", resource), e);
        }
    }

    private static String normalisePath(String path) {
        return path.startsWith("/") ? path : "/".concat(path);
    }

    private void installMouseHandling() {
        var mouse = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                anchor = findTile(e.getPoint());
                updateSelection(new Rectangle(anchor.x, anchor.y, 1, 1));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateSelection(buildSelection(anchor, findTile(e.getPoint())));
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /**
     * Converts a point in component pixels to the tile containing it, clamped to the grid so that a drag beyond
     * the edge selects the outermost row or column.
     */
    private Point findTile(Point point) {
        var column = clampToRange(point.x / tileWidth,  columns - 1);
        var row    = clampToRange(point.y / tileHeight, rows    - 1);
        return new Point(column, row);
    }

    private int clampToRange(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }

    private Rectangle buildSelection(Point from, Point to) {
        return new Rectangle(
                Math.min(from.x, to.x),
                Math.min(from.y, to.y),
                Math.abs(from.x - to.x) + 1,
                Math.abs(from.y - to.y) + 1
        );
    }

    private void updateSelection(Rectangle tiles) {
        if (tiles.equals(selection)) {
            return;
        }

        selection = tiles;
        repaint();
        fireSelectionChanged();
    }

    private void fireSelectionChanged() {
        var current = new Rectangle(selection);
        for (var listener : listeners) {
            listener.handleSelection(current);
        }
    }

    private void paintGrid(Graphics2D canvas) {
        // the final line of each axis is pulled inside the image, where it stays visible
        var right  = columns * tileWidth  - 1;
        var bottom = rows    * tileHeight - 1;

        canvas.setColor(GRID_COLOUR);

        for (var column = 0; column <= columns; column++) {
            var x = Math.min(column * tileWidth, right);
            canvas.drawLine(x, 0, x, bottom);
        }

        for (var row = 0; row <= rows; row++) {
            var y = Math.min(row * tileHeight, bottom);
            canvas.drawLine(0, y, right, y);
        }
    }

    private void paintSelection(Graphics2D canvas) {
        var bounds = resolveSelectionBounds();

        canvas.setColor(SELECTION_FILL);
        canvas.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        canvas.setColor(SELECTION_OUTLINE);
        canvas.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
    }

    private Rectangle resolveSelectionBounds() {
        return new Rectangle(
                selection.x      * tileWidth,
                selection.y      * tileHeight,
                selection.width  * tileWidth,
                selection.height * tileHeight
        );
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * Receives the tiles a {@link Swatch} selects, so a component beside the swatch can follow the selection.
     */
    @FunctionalInterface
    public interface SelectionListener {

        /**
         * Accepts the tiles now selected.
         *
         * @param selection the selected tiles in tile coordinates, spanning at least one tile
         */
        void handleSelection(Rectangle selection);
    }

}
