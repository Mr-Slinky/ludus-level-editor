package com.slinky.ludus.editor.components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JPanel;

/**
 * A stack of {@value #MAX_LAYERS} grids of cells the same size as the tiles in a {@link Swatch}, onto which a
 * caller stamps tiles. Every layer shares one row and column count, and the panel takes exactly the pixel size
 * of that grid, as both its preferred and its minimum size. A layout with room to spare therefore centres the
 * canvas, and a layout with less room than the grid needs clips it, which keeps the grid on screen at whatever
 * size the window happens to be.
 * <p>
 * The layers paint in index order, so layer 0 goes down first and each layer after it draws over the one below.
 * A tile's transparent pixels let the layers beneath show through, which is what puts the overhanging face of a
 * cliff over the ground behind it. Every layer stays fully visible at every moment, so a level appears while a
 * user edits it exactly as it will appear once it is finished.
 * <p>
 * One layer at a time takes a stamp. {@link #setActiveLayer(int)} chooses it, {@link #getActiveLayer()} reports
 * it, and {@link #getGrid()} returns its tiles. A press writes to that layer alone and leaves the rest as they
 * stand.
 * <p>
 * Every cell starts as water, drawn in the colour read from {@value #WATER_ASSET} beneath layer 0, and a stamped
 * tile covers that colour. A level therefore begins as an expanse of water that a user builds land into, and
 * {@link #getWaterColour()} returns the colour a caller writing the level out needs for the cells nothing was
 * stamped onto.
 * <p>
 * A caller arms a tile through {@link #setArmedTile(BufferedImage)}, and a press then writes that tile into the
 * cell under the pointer. The armed tile stays armed, so one tile can be stamped as many times as a user likes.
 * While a tile is armed, the cell under the pointer shows that tile at reduced opacity inside an outline, so the
 * destination of a press is visible before the press.
 * <p>
 * {@link TileGrid} stores the placed tiles of one layer. A resize goes through {@link #resizeGrid(int, int)},
 * which sets every layer at once. Growing keeps every tile where it is, and shrinking keeps the tiles that stay
 * inside the new bounds. A shrink that would drop a tile is refused, so a resize leaves no tile stranded outside
 * the grid, and {@link #canResizeTo(int, int)} answers in advance which of those a given size would be.
 * <p>
 * <b>Stamping a cliff face over the ground behind it</b>
 * <pre>{@code
 * var canvas = new LevelCanvas(64, 15, 15);
 *
 * // 15 columns of 64 pixels by 15 rows of 64 pixels
 * canvas.getPreferredSize();          // java.awt.Dimension[width=960,height=960]
 *
 * swatch.readSelectedImage().ifPresent(canvas::setArmedTile);
 *
 * // a press at (300, 140) writes the armed tile into column 4, row 2 of layer 0
 * canvas.getGrid().readTile(4, 2);    // the armed image
 *
 * canvas.setActiveLayer(1);
 *
 * // a press on the same cell now writes to layer 1, and layer 0 keeps its tile
 * canvas.getLayer(0).readTile(4, 2);  // still the first image
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class LevelCanvas extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The asset the water colour is read from, below {@value Swatch#ROOT_DIR}. */
    public static final String WATER_ASSET = "terrain/tilesets/water-background-color.png";

    /** The number of grids a canvas stacks, indexed 0 at the bottom to {@code MAX_LAYERS - 1} at the top. */
    public static final int MAX_LAYERS = 6;

    private static final Color BACKGROUND    = new Color(247, 247, 249);
    private static final Color GRID_COLOUR   = new Color(0, 0, 0, 55);
    private static final Color HOVER_OUTLINE = new Color(255, 214, 0);

    private static final float HOVER_ALPHA = 0.45f;

    /**
     * Returns the colour filling {@value #WATER_ASSET}, taken from its top left pixel. The asset is a single
     * flat colour, so any pixel in it answers the same.
     */
    private static Color readWaterColour() {
        var image = Swatch.loadImage(WATER_ASSET);

        return new Color(image.getRGB(0, 0), true);
    }

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<TileGrid> layers = new ArrayList<>();
    private final Color waterColour = readWaterColour();
    private final int cellWidth;
    private final int cellHeight;
    private final int maxColumns;
    private final int maxRows;

    private int           activeLayer;
    private BufferedImage armedTile;
    private Point         hovered;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a canvas of square cells, with the grid starting at its largest.
     *
     * @param cellSize   the cell width and height in pixels
     * @param maxColumns the widest the grid can be, in cells
     * @param maxRows    the tallest the grid can be, in cells
     */
    public LevelCanvas(int cellSize, int maxColumns, int maxRows) {
        this(cellSize, cellSize, maxColumns, maxRows);
    }

    /**
     * Builds a canvas of cells of the given size, with the grid starting at its largest.
     *
     * @param cellWidth  the cell width in pixels
     * @param cellHeight the cell height in pixels
     * @param maxColumns the widest the grid can be, in cells
     * @param maxRows    the tallest the grid can be, in cells
     * @throws IllegalArgumentException if any argument is zero or negative
     */
    public LevelCanvas(int cellWidth, int cellHeight, int maxColumns, int maxRows) {
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException(String.format("Cell size must be positive, given %d by %d", cellWidth, cellHeight));
        }

        if (maxColumns <= 0 || maxRows <= 0) {
            throw new IllegalArgumentException(String.format("The grid maximum must be positive, given %d by %d", maxColumns, maxRows));
        }

        this.cellWidth  = cellWidth;
        this.cellHeight = cellHeight;
        this.maxColumns = maxColumns;
        this.maxRows    = maxRows;

        for (var layer = 0; layer < MAX_LAYERS; layer++) {
            layers.add(new TileGrid(maxColumns, maxRows));
        }

        applyGridSize();
        setBackground(BACKGROUND);
        installMouseHandling();
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /**
     * Returns the tiles of the layer a press currently stamps onto.
     *
     * @return the active layer's grid
     */
    public TileGrid getGrid() {
        return readActiveGrid();
    }

    /**
     * Returns the tiles of one layer, active or otherwise.
     *
     * @param layer the index, from 0 at the bottom to {@code MAX_LAYERS - 1} at the top
     * @return that layer's grid
     * @throws IndexOutOfBoundsException if the index addresses no layer
     */
    public TileGrid getLayer(int layer) {
        requireLayer(layer);

        return layers.get(layer);
    }

    /**
     * Returns the index of the layer a press currently stamps onto.
     *
     * @return the active index, from 0 at the bottom to {@code MAX_LAYERS - 1} at the top
     */
    public int getActiveLayer() {
        return activeLayer;
    }

    public int getLayerCount() {
        return layers.size();
    }

    public int getColumns() {
        return readActiveGrid().getColumns();
    }

    public int getRows() {
        return readActiveGrid().getRows();
    }

    /**
     * Returns the colour every cell shows before a tile lands on it.
     *
     * @return the colour read from {@value #WATER_ASSET}
     */
    public Color getWaterColour() {
        return waterColour;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public int getMaxColumns() {
        return maxColumns;
    }

    public int getMaxRows() {
        return maxRows;
    }

    /**
     * Returns the tile a press will stamp.
     *
     * @return the armed tile, and empty while no tile is armed
     */
    public Optional<BufferedImage> getArmedTile() {
        return Optional.ofNullable(armedTile);
    }

    // ========================================================================================== \\
    //                                          Setters                                           \\
    // ========================================================================================== \\
    /**
     * Arms a tile, which every later press stamps into the cell under the pointer until another tile replaces it.
     *
     * @param tile the image to stamp
     * @throws IllegalArgumentException if the tile is null
     */
    public void setArmedTile(BufferedImage tile) {
        if (tile == null) {
            throw new IllegalArgumentException("An armed tile requires an image");
        }

        armedTile = tile;
        repaint();
    }

    /**
     * Chooses the layer every later press stamps onto. The other layers keep their tiles and stay visible, so
     * the change alters where a stamp lands and leaves the picture alone.
     *
     * @param layer the index, from 0 at the bottom to {@code MAX_LAYERS - 1} at the top
     * @throws IndexOutOfBoundsException if the index addresses no layer
     */
    public void setActiveLayer(int layer) {
        requireLayer(layer);

        activeLayer = layer;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Disarms the tile, so that a press leaves the grid as it stands.
     */
    public void clearArmedTile() {
        armedTile = null;
        repaint();
    }

    /**
     * Empties every cell of the active layer and repaints, leaving the other layers as they stand.
     */
    public void clearGrid() {
        readActiveGrid().clear();
        repaint();
    }

    /**
     * Empties every cell of every layer and repaints, which also leaves every size within the maximum available
     * to {@link #resizeGrid(int, int)}.
     */
    public void clearAllLayers() {
        layers.forEach(TileGrid::clear);
        repaint();
    }

    /**
     * Resizes every layer and lays the canvas out again at the new pixel size. All layers share one size, so a
     * resize either sets all of them or sets none. Every tile inside the new bounds keeps its cell.
     *
     * @param columns the new width in cells
     * @param rows    the new height in cells
     * @throws IllegalStateException    if a tile stands outside the new bounds on any layer
     * @throws IllegalArgumentException if the size falls outside one cell to the maximum
     */
    public void resizeGrid(int columns, int rows) {
        requireSize(columns, rows);

        if (canResizeTo(columns, rows) == false) {
            throw new IllegalStateException(String.format("A size of %d by %d would drop %d tiles", columns, rows, countTilesOutside(columns, rows)));
        }

        layers.forEach(layer -> layer.resize(columns, rows));
        applyGridSize();
        revalidate();
        repaint();
    }

    /**
     * Reports whether a resize would keep every tile currently placed.
     *
     * @param columns the width to test, in cells
     * @param rows    the height to test, in cells
     * @return {@code true} while every tile on every layer stands inside those bounds
     * @throws IllegalArgumentException if the size falls outside one cell to the maximum
     */
    public boolean canResizeTo(int columns, int rows) {
        requireSize(columns, rows);

        return countTilesOutside(columns, rows) == 0;
    }

    /**
     * Counts the cells holding a tile across every layer.
     *
     * @return the number of tiles placed, from zero to the cell count times the layer count
     */
    public int countPlacedTiles() {
        return layers.stream()
                     .mapToInt(TileGrid::getPlacedCount)
                     .sum();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        var canvas = (Graphics2D) g.create();
        paintWater(canvas);
        paintTiles(canvas);
        paintGrid(canvas);
        paintHover(canvas);
        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private void installMouseHandling() {
        var mouse = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                stampAt(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                trackHover(findCell(e.getPoint()).orElse(null));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                trackHover(findCell(e.getPoint()).orElse(null));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                trackHover(null);
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /**
     * Writes the armed tile into the cell of the active layer containing the point, leaving every layer alone
     * while no tile is armed or the point falls outside the grid.
     */
    private void stampAt(Point point) {
        if (armedTile == null) {
            return;
        }

        findCell(point).ifPresent(cell -> {
            readActiveGrid().placeTile(cell.x, cell.y, armedTile);
            repaint();
        });
    }

    /**
     * Converts a point in component pixels to the cell containing it.
     *
     * @return the cell, and empty for a point outside the grid
     */
    private Optional<Point> findCell(Point point) {
        if (point.x < 0 || point.y < 0) {
            return Optional.empty();
        }

        var column = point.x / cellWidth;
        var row    = point.y / cellHeight;

        return readActiveGrid().contains(column, row) ? Optional.of(new Point(column, row)) : Optional.empty();
    }

    /**
     * Remembers the cell under the pointer and repaints once it changes, which keeps the ghost to one repaint per
     * cell crossed rather than one per pixel moved.
     */
    private void trackHover(Point cell) {
        if (Objects.equals(cell, hovered)) {
            return;
        }

        hovered = cell;
        repaint();
    }

    /**
     * Fills the grid with the water colour, which every later stage paints over.
     */
    private void paintWater(Graphics2D canvas) {
        canvas.setColor(waterColour);
        canvas.fillRect(0, 0, getColumns() * cellWidth, getRows() * cellHeight);
    }

    /**
     * Draws every layer in index order, so layer 0 goes down first and each layer after it covers the one below
     * wherever its tiles are opaque.
     */
    private void paintTiles(Graphics2D canvas) {
        for (var layer : layers) {
            paintLayer(canvas, layer);
        }
    }

    private void paintLayer(Graphics2D canvas, TileGrid layer) {
        for (var row = 0; row < layer.getRows(); row++) {
            for (var column = 0; column < layer.getColumns(); column++) {
                var x = column * cellWidth;
                var y = row * cellHeight;

                layer.readTile(column, row)
                     .ifPresent(tile -> canvas.drawImage(tile, x, y, cellWidth, cellHeight, null));
            }
        }
    }

    private void paintGrid(Graphics2D canvas) {
        var right  = getColumns() * cellWidth  - 1;
        var bottom = getRows()    * cellHeight - 1;

        canvas.setColor(GRID_COLOUR);

        for (var column = 0; column <= getColumns(); column++) {
            var x = Math.min(column * cellWidth, right);
            canvas.drawLine(x, 0, x, bottom);
        }

        for (var row = 0; row <= getRows(); row++) {
            var y = Math.min(row * cellHeight, bottom);
            canvas.drawLine(0, y, right, y);
        }
    }

    /**
     * Draws the armed tile at reduced opacity in the cell under the pointer, with an outline around it, so a user
     * sees where a press will land before making it.
     */
    private void paintHover(Graphics2D canvas) {
        if (armedTile == null || hovered == null) {
            return;
        }

        var x = hovered.x * cellWidth;
        var y = hovered.y * cellHeight;

        var ghost = (Graphics2D) canvas.create();
        ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, HOVER_ALPHA));
        ghost.drawImage(armedTile, x, y, cellWidth, cellHeight, null);
        ghost.dispose();

        canvas.setColor(HOVER_OUTLINE);
        canvas.drawRect(x, y, cellWidth - 1, cellHeight - 1);
    }

    /**
     * Sets the panel's preferred and minimum size to the pixel size of the grid. Both matter: a layout with room
     * to spare reads the preferred size and centres the canvas, and a layout with less room reads the minimum
     * size, which keeps the grid drawn and clipped rather than shrunk away to nothing.
     */
    private void applyGridSize() {
        var size = new Dimension(getColumns() * cellWidth, getRows() * cellHeight);

        setPreferredSize(size);
        setMinimumSize(size);
    }

    /**
     * Counts the tiles standing outside the given bounds, across every layer.
     */
    private int countTilesOutside(int columns, int rows) {
        var outside = 0;

        for (var layer : layers) {
            for (var row = 0; row < layer.getRows(); row++) {
                for (var column = 0; column < layer.getColumns(); column++) {
                    if ((column >= columns || row >= rows) && layer.readTile(column, row).isPresent()) {
                        outside++;
                    }
                }
            }
        }

        return outside;
    }

    private void requireSize(int columns, int rows) {
        if (columns < 1 || columns > maxColumns || rows < 1 || rows > maxRows) {
            throw new IllegalArgumentException(String.format("A size of %d by %d falls outside 1 by 1 to %d by %d", columns, rows, maxColumns, maxRows));
        }
    }

    /**
     * Returns the grid a press stamps onto, which every method reaching for the current layer goes through.
     */
    private TileGrid readActiveGrid() {
        return layers.get(activeLayer);
    }

    private void requireLayer(int layer) {
        if (layer < 0 || layer >= layers.size()) {
            throw new IndexOutOfBoundsException(String.format("A stack of %d layers has no layer %d", layers.size(), layer));
        }
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * The tiles of one layer, addressed by column and row. A grid takes a new size at any time and keeps the
     * tiles the new bounds still contain, so growing a level costs nothing and shrinking one costs the tiles
     * standing in the cells that go.
     */
    public class TileGrid {

        private BufferedImage[][] tiles;
        private int columns;
        private int rows;
        private int placed;

        private TileGrid(int columns, int rows) {
            this.columns = columns;
            this.rows    = rows;
            this.tiles   = new BufferedImage[rows][columns];
        }

        public int getColumns() {
            return columns;
        }

        public int getRows() {
            return rows;
        }

        /**
         * Counts the cells holding a tile.
         *
         * @return the number of tiles placed, from zero to the cell count
         */
        public int getPlacedCount() {
            return placed;
        }

        /**
         * Reports whether every cell is free.
         *
         * @return {@code true} while no tile has been placed
         */
        public boolean isEmpty() {
            return placed == 0;
        }

        /**
         * Reports whether a column and row pair addresses a cell in this grid.
         *
         * @param column the column to test
         * @param row    the row to test
         * @return {@code true} while both fall inside the grid
         */
        public boolean contains(int column, int row) {
            return column >= 0 && column < columns && row >= 0 && row < rows;
        }

        /**
         * Returns the tile in one cell.
         *
         * @param column the column to read
         * @param row    the row to read
         * @return the tile in that cell, and empty for a free cell or a pair outside the grid
         */
        public Optional<BufferedImage> readTile(int column, int row) {
            return contains(column, row) ? Optional.ofNullable(tiles[row][column]) : Optional.empty();
        }

        /**
         * Writes a tile into one cell, replacing whatever that cell held.
         *
         * @param column the column to write
         * @param row    the row to write
         * @param tile   the image to store
         * @throws IndexOutOfBoundsException if the pair falls outside the grid
         * @throws IllegalArgumentException  if the tile is null
         */
        public void placeTile(int column, int row, BufferedImage tile) {
            requireInside(column, row);

            if (tile == null) {
                throw new IllegalArgumentException("A placed tile requires an image");
            }

            if (tiles[row][column] == null) {
                placed++;
            }

            tiles[row][column] = tile;
        }

        /**
         * Frees one cell.
         *
         * @param column the column to free
         * @param row    the row to free
         * @throws IndexOutOfBoundsException if the pair falls outside the grid
         */
        public void removeTile(int column, int row) {
            requireInside(column, row);

            if (tiles[row][column] != null) {
                placed--;
            }

            tiles[row][column] = null;
        }

        /**
         * Frees every cell, which returns the grid to a state that accepts a new size.
         */
        public void clear() {
            tiles  = new BufferedImage[rows][columns];
            placed = 0;
        }

        /**
         * Sets the row and column count, keeping every tile that stands inside both the old bounds and the new
         * ones. A tile outside the new bounds is dropped, so a caller shrinking a grid checks
         * {@link LevelCanvas#canResizeTo(int, int)} first.
         *
         * @param columns the new width in cells
         * @param rows    the new height in cells
         * @throws IllegalArgumentException if either count falls outside one cell to the maximum
         */
        public void resize(int columns, int rows) {
            requireSize(columns, rows);

            var resized = new BufferedImage[rows][columns];
            var kept    = 0;

            for (var row = 0; row < Math.min(rows, this.rows); row++) {
                for (var column = 0; column < Math.min(columns, this.columns); column++) {
                    resized[row][column] = tiles[row][column];

                    if (resized[row][column] != null) {
                        kept++;
                    }
                }
            }

            this.columns = columns;
            this.rows    = rows;
            this.tiles   = resized;
            this.placed  = kept;
        }

        private void requireInside(int column, int row) {
            if (contains(column, row) == false) {
                throw new IndexOutOfBoundsException(String.format("A grid of %d by %d has no cell at column %d, row %d", columns, rows, column, row));
            }
        }
    }

}
