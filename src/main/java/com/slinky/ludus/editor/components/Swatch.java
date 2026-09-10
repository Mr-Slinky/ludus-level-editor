package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;

import java.awt.BasicStroke;
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
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Takes a single image and turns it into a swatch of selectable tiles, drawn at the image's natural size. The
 * tile width and height determine how many rows and columns this swatch has. An image 192 pixels wide and 64
 * tall, cut into 64 pixel tiles, yields one row and three columns.
 * <p>
 * A press selects the one tile under the pointer. {@link #getSelection()} returns that tile in tile
 * coordinates, where {@code x} is the column and {@code y} is the row. A new swatch starts with an empty
 * selection, and {@link #clearSelection()} returns it to that state.
 * <p>
 * <b>Keeping one selection across several swatches</b>
 * <p>
 * {@link #clearSelection()} leaves listeners unnotified, so a container can clear its other swatches from
 * inside a listener without those clears arriving back as further selections.
 * <pre>{@code
 * var terrain = new Swatch(terrainPath, 64);
 * var water   = new Swatch(waterPath, 64);
 *
 * terrain.addSelectionListener(selection -> water.clearSelection());
 * water.addSelectionListener(selection -> terrain.clearSelection());
 *
 * // a press on the first tile of the second row of the terrain swatch leaves
 * // terrain.getSelection() equal to Optional[java.awt.Point[x=0,y=1]]
 * // water.getSelection()   equal to Optional.empty
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public class Swatch extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    public static final String ROOT_DIR = "/assets";

    private static final Color BACKGROUND        = Palette.getActive().getDark();
    private static final Color GRID_COLOUR       = Palette.getActive().getGridLine();
    private static final Color SELECTION_FILL    = Palette.withAlpha(Palette.getActive().getAccent1(), 60);
    private static final Color SELECTION_OUTLINE = Palette.getActive().getAccent1();

    /** The width in pixels of the outline round the selected tile, drawn inside the cell. */
    private static final int SELECTION_STROKE = 2;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<SelectionListener> listeners = new ArrayList<>();
    private final BufferedImage image;
    private final String resourcePath;
    private final int tileWidth;
    private final int tileHeight;
    private final int rows;
    private final int columns;

    private Point selection;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Loads an image from the assets directory and cuts it into square tiles.
     *
     * @param imagePath the path below {@value #ROOT_DIR}, with or without a leading slash
     * @param dimension the tile width and height in pixels
     * @throws IllegalArgumentException if the classpath contains no resource at that path, if the resource
     *                                  decodes to no image, if the dimension is zero or negative, or if the
     *                                  image is smaller than a single tile
     * @throws UncheckedIOException     if reading the resource fails
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
     * @throws IllegalArgumentException if the classpath contains no resource at that path, if the resource
     *                                  decodes to no image, if either tile dimension is zero or negative, or
     *                                  if the image is smaller than a single tile
     * @throws UncheckedIOException     if reading the resource fails
     */
    public Swatch(String imagePath, int tileWidth, int tileHeight) {
        this(loadImage(imagePath), resolveResourcePath(imagePath), tileWidth, tileHeight);
    }

    /**
     * Cuts an image already in memory into square tiles.
     *
     * @param image     the image to draw and divide
     * @param dimension the tile width and height in pixels
     * @throws IllegalArgumentException if the image is null, if the dimension is zero or negative, or if the
     *                                  image is smaller than a single tile
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
        this(image, null, tileWidth, tileHeight);
    }

    private Swatch(BufferedImage image, String resourcePath, int tileWidth, int tileHeight) {
        if (image == null) {
            throw new IllegalArgumentException("A swatch requires an image");
        }

        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException(String.format("Tile size must be positive, given %d by %d", tileWidth, tileHeight));
        }

        this.image        = image;
        this.resourcePath = resourcePath;
        this.tileWidth    = tileWidth;
        this.tileHeight   = tileHeight;
        this.rows         = image.getHeight() / tileHeight;
        this.columns      = image.getWidth()  / tileWidth;

        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException(String.format("An image of %d by %d fits no tile of %d by %d", image.getWidth(), image.getHeight(), tileWidth, tileHeight));
        }

        var size = new Dimension(image.getWidth(), image.getHeight());

        // a tileset's transparent cells show the background through, so the swatch keeps an edge against the
        // panel behind it
        setBackground(BACKGROUND);

        // the minimum matters as much as the preferred size: a layout short of room reads the minimum, and a
        // swatch that states none shrinks away to nothing instead of being clipped
        setPreferredSize(size);
        setMinimumSize(size);
        installMouseHandling();
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * Returns the classpath resource that this swatch loaded its image from, starting at the classpath root, so
     * a swatch built over {@code terrain/tilesets/tilemap_color1.png} answers
     * {@code /assets/terrain/tilesets/tilemap_color1.png}.
     *
     * @return that resource path, and empty for a swatch built over an image already in memory
     */
    public Optional<String> getResourcePath() {
        return Optional.ofNullable(resourcePath);
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    /**
     * Returns the selected tile in tile coordinates, where {@code x} is the column and {@code y} is the row.
     *
     * @return a copy of the selected tile, and empty while nothing is selected
     */
    public Optional<Point> getSelection() {
        return Optional.ofNullable(selection).map(Point::new);
    }

    /**
     * Reports whether a tile is selected.
     *
     * @return {@code true} while one tile is selected
     */
    public boolean hasSelection() {
        return selection != null;
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
     * Returns the region of the source image that the selected tile covers, at the image's own resolution. A swatch
     * of 64 pixel tiles returns a 64 by 64 image.
     *
     * @return a view onto the source image, sharing its pixels, and empty while nothing is selected
     */
    public Optional<BufferedImage> readSelectedImage() {
        return resolveSelectionBounds().map(bounds -> image.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height));
    }

    /**
     * Empties the selection and repaints, leaving every listener unnotified. A container holding several swatches
     * calls this on the others when one of them reports a selection, so that one tile stays selected across the
     * whole set.
     */
    public void clearSelection() {
        if (selection == null) {
            return;
        }

        selection = null;
        repaint();
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
    /**
     * Reads an image out of the assets directory on the classpath.
     *
     * @param path the path below {@value #ROOT_DIR}, with or without a leading slash
     * @return the decoded image
     * @throws IllegalArgumentException if the classpath contains no resource at that path, or the resource
     *                                  decodes to no image
     * @throws UncheckedIOException     if reading the resource fails
     */
    static BufferedImage loadImage(String path) {
        var resource = resolveResourcePath(path);

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

    /**
     * Places a given path below {@value #ROOT_DIR}, which is both the resource that {@link #loadImage(String)}
     * reads and the path that {@link #getResourcePath()} answers with.
     *
     * @param path the path below {@value #ROOT_DIR}, with or without a leading slash
     * @return that path from the classpath root
     */
    static String resolveResourcePath(String path) {
        return ROOT_DIR + normalisePath(path);
    }

    private static String normalisePath(String path) {
        return path.startsWith("/") ? path : "/".concat(path);
    }

    private void installMouseHandling() {
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                updateSelection(findTile(e.getPoint()));
            }
        });
    }

    /**
     * Converts a point in component pixels to the tile containing it, clamped to the grid so that a press beyond
     * the edge selects the outermost row or column.
     */
    private Point findTile(Point point) {
        var row    = clampToRange(point.y / tileHeight, rows    - 1);
        var column = clampToRange(point.x / tileWidth,  columns - 1);
        return new Point(column, row);
    }

    private int clampToRange(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }

    private void updateSelection(Point tile) {
        if (tile.equals(selection)) {
            return;
        }

        selection = tile;
        repaint();
        fireSelectionChanged();
    }

    private void fireSelectionChanged() {
        var current = new Point(selection);
        for (var listener : listeners) {
            listener.handleSelection(current);
        }
    }

    private void paintGrid(Graphics2D canvas) {
        // the final line of each axis is pulled inside the image, where it stays visible
        var right  = columns * tileWidth  - 1;
        var bottom = rows    * tileHeight - 1;

        canvas.setColor(GRID_COLOUR);

        for (var row = 0; row <= rows; row++) {
            var y = Math.min(row * tileHeight, bottom);
            canvas.drawLine(0, y, right, y);
        }

        for (var column = 0; column <= columns; column++) {
            var x = Math.min(column * tileWidth, right);
            canvas.drawLine(x, 0, x, bottom);
        }
    }

    private void paintSelection(Graphics2D canvas) {
        var bounds = resolveSelectionBounds();
        if (bounds.isEmpty()) {
            return;
        }

        var region = bounds.get();

        canvas.setColor(SELECTION_FILL);
        canvas.fillRect(region.x, region.y, region.width, region.height);

        // the outline draws inside the cell, so the stroke is inset by half its width at every edge
        var inset = SELECTION_STROKE / 2;

        canvas.setColor(SELECTION_OUTLINE);
        canvas.setStroke(new BasicStroke(SELECTION_STROKE));
        canvas.drawRect(
                region.x + inset,
                region.y + inset,
                region.width  - SELECTION_STROKE,
                region.height - SELECTION_STROKE
        );
    }

    /**
     * Converts the selected tile from tile coordinates to the pixel rectangle that it covers in the source image.
     */
    private Optional<Rectangle> resolveSelectionBounds() {
        return Optional.ofNullable(selection)
                       .map(tile -> new Rectangle(
                               tile.x * tileWidth,
                               tile.y * tileHeight,
                               tileWidth,
                               tileHeight
                       ));
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * Receives the tile that a {@link Swatch} selects, so a component beside the swatch can follow the selection.
     */
    @FunctionalInterface
    public interface SelectionListener {

        /**
         * Accepts the tile now selected.
         *
         * @param selection the selected tile in tile coordinates, with {@code x} the column and {@code y} the row
         */
        void handleSelection(Point selection);
    }

}
