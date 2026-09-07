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
import java.util.Objects;
import java.util.Optional;

import javax.swing.JPanel;

/**
 * A grid of cells the same size as the tiles in a {@link Swatch}, onto which a caller stamps tiles. The panel
 * takes the pixel size of the largest grid it will ever hold, so a smaller grid centres inside a panel whose
 * dimensions stay put.
 * <p>
 * Every cell starts as water, drawn in the colour read from {@value #WATER_ASSET}, and a stamped tile covers
 * that colour. A level therefore begins as an expanse of water that a user builds land into, and
 * {@link #getWaterColour()} returns the colour a caller writing the level out needs for the cells nothing was
 * stamped onto.
 * <p>
 * A caller arms a tile through {@link #setArmedTile(BufferedImage)}, and a press then writes that tile into the
 * cell under the pointer. The armed tile stays armed, so one tile can be stamped as many times as a user likes.
 * While a tile is armed, the cell under the pointer shows that tile at reduced opacity inside an outline, so the
 * destination of a press is visible before the press.
 * <p>
 * {@link TileGrid} holds the placed tiles and accepts a new row and column count only while it holds none of
 * them, which is what keeps a resize from stranding tiles outside the grid.
 * <p>
 * <b>Stamping a tile from a swatch</b>
 * <pre>{@code
 * var canvas = new LevelCanvas(64, 15, 15);
 *
 * // 15 columns of 64 pixels by 15 rows of 64 pixels
 * canvas.getPreferredSize();          // java.awt.Dimension[width=960,height=960]
 *
 * swatch.readSelectedImage().ifPresent(canvas::setArmedTile);
 *
 * // a press at (300, 140) writes the armed tile into column 4, row 2
 * canvas.getGrid().readTile(4, 2);    // the armed image
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
    public static final String WATER_ASSET = "terrain/tilesets/Water Background color.png";

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
    private final TileGrid grid;
    private final Color waterColour = readWaterColour();
    private final int cellWidth;
    private final int cellHeight;
    private final int maxColumns;
    private final int maxRows;

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
        this.grid       = new TileGrid(maxColumns, maxRows);

        setPreferredSize(new Dimension(maxColumns * cellWidth, maxRows * cellHeight));
        setBackground(BACKGROUND);
        installMouseHandling();
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public TileGrid getGrid() {
        return grid;
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
     * Empties every cell and repaints, which also returns the grid to a state that accepts a new size.
     */
    public void clearGrid() {
        grid.clear();
        repaint();
    }

    /**
     * Resizes the grid and repaints, keeping it centred in the canvas.
     *
     * @param columns the new width in cells
     * @param rows    the new height in cells
     * @throws IllegalStateException    if the grid holds a tile
     * @throws IllegalArgumentException if the size falls outside one cell to the maximum
     */
    public void resizeGrid(int columns, int rows) {
        grid.resize(columns, rows);
        repaint();
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
     * Writes the armed tile into the cell containing the point, leaving the grid alone while no tile is armed or
     * the point falls outside the grid.
     */
    private void stampAt(Point point) {
        if (armedTile == null) {
            return;
        }

        findCell(point).ifPresent(cell -> {
            grid.placeTile(cell.x, cell.y, armedTile);
            repaint();
        });
    }

    /**
     * Converts a point in component pixels to the cell containing it.
     *
     * @return the cell, and empty for a point outside the grid
     */
    private Optional<Point> findCell(Point point) {
        var offsetX = readOffsetX();
        var offsetY = readOffsetY();

        if (point.x < offsetX || point.y < offsetY) {
            return Optional.empty();
        }

        var column = (point.x - offsetX) / cellWidth;
        var row    = (point.y - offsetY) / cellHeight;

        return grid.contains(column, row) ? Optional.of(new Point(column, row)) : Optional.empty();
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
     * Fills the grid with the water colour, which every later stage paints over. The fill stops at the grid, so
     * a grid smaller than the maximum leaves the canvas around it in the panel background and the level's own
     * extent stays visible.
     */
    private void paintWater(Graphics2D canvas) {
        canvas.setColor(waterColour);
        canvas.fillRect(readOffsetX(), readOffsetY(), grid.getColumns() * cellWidth, grid.getRows() * cellHeight);
    }

    private void paintTiles(Graphics2D canvas) {
        var offsetX = readOffsetX();
        var offsetY = readOffsetY();

        for (var row = 0; row < grid.getRows(); row++) {
            for (var column = 0; column < grid.getColumns(); column++) {
                var x = offsetX + column * cellWidth;
                var y = offsetY + row * cellHeight;

                grid.readTile(column, row)
                    .ifPresent(tile -> canvas.drawImage(tile, x, y, cellWidth, cellHeight, null));
            }
        }
    }

    private void paintGrid(Graphics2D canvas) {
        var offsetX = readOffsetX();
        var offsetY = readOffsetY();
        var right   = offsetX + grid.getColumns() * cellWidth  - 1;
        var bottom  = offsetY + grid.getRows()    * cellHeight - 1;

        canvas.setColor(GRID_COLOUR);

        for (var column = 0; column <= grid.getColumns(); column++) {
            var x = Math.min(offsetX + column * cellWidth, right);
            canvas.drawLine(x, offsetY, x, bottom);
        }

        for (var row = 0; row <= grid.getRows(); row++) {
            var y = Math.min(offsetY + row * cellHeight, bottom);
            canvas.drawLine(offsetX, y, right, y);
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

        var x = readOffsetX() + hovered.x * cellWidth;
        var y = readOffsetY() + hovered.y * cellHeight;

        var ghost = (Graphics2D) canvas.create();
        ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, HOVER_ALPHA));
        ghost.drawImage(armedTile, x, y, cellWidth, cellHeight, null);
        ghost.dispose();

        canvas.setColor(HOVER_OUTLINE);
        canvas.drawRect(x, y, cellWidth - 1, cellHeight - 1);
    }

    /**
     * Returns the pixels between the left edge of the canvas and the left edge of the grid, which centres a grid
     * narrower than the maximum.
     */
    private int readOffsetX() {
        return (maxColumns - grid.getColumns()) * cellWidth / 2;
    }

    private int readOffsetY() {
        return (maxRows - grid.getRows()) * cellHeight / 2;
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * The tiles a {@link LevelCanvas} holds, addressed by column and row. A grid accepts a new size only while it
     * holds no tiles, so the row and column counts settle the moment the first tile lands and stay settled until
     * {@link #clear()} empties the grid again.
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
         * Sets the row and column count.
         *
         * @param columns the new width in cells
         * @param rows    the new height in cells
         * @throws IllegalStateException    if the grid holds a tile
         * @throws IllegalArgumentException if either count falls outside one cell to the maximum
         */
        public void resize(int columns, int rows) {
            if (isEmpty() == false) {
                throw new IllegalStateException(String.format("A grid holding %d tiles keeps its size of %d by %d", placed, this.columns, this.rows));
            }

            if (columns < 1 || columns > maxColumns || rows < 1 || rows > maxRows) {
                throw new IllegalArgumentException(String.format("A size of %d by %d falls outside 1 by 1 to %d by %d", columns, rows, maxColumns, maxRows));
            }

            this.columns = columns;
            this.rows    = rows;
            this.tiles   = new BufferedImage[rows][columns];
        }

        private void requireInside(int column, int row) {
            if (contains(column, row) == false) {
                throw new IndexOutOfBoundsException(String.format("A grid of %d by %d has no cell at column %d, row %d", columns, rows, column, row));
            }
        }
    }

}
