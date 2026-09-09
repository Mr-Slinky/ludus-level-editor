package com.slinky.ludus.editor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The tiles that make up one layer of a level, addressed by row and column. Every occupied cell stores one
 * {@link TileSource}, which records where the art was cut from as well as the pixels to draw.
 * <p>
 * Every method below takes a position as a row index followed by a column index. Both indices count from 0, and
 * {@link #contains(int, int)} returns whether a pair addresses a cell of this grid.
 * <p>
 * A caller sets a new size through {@link #resize(int, int)}, which keeps every tile inside both the old bounds
 * and the new ones and drops the rest. {@link #countTilesOutside(int, int)} returns how many tiles a given size
 * would drop, so a caller tests a size before applying it.
 * <p>
 * {@link #readPlacedTiles()} returns one {@link PlacedTile} for each occupied cell. Each of those states where
 * one tile stands on the grid, the {@link TileSet} that its art was cut from, and the cell that it occupies
 * inside that tileset.
 * <p>
 * <b>Stamping a tile and reading the layer back</b>
 * <p>
 * A caller places one tile on a new grid, then reads the cells back:
 * <pre>{@code
 * void stampGrass(BufferedImage art) {
 *     var grid    = new TileGrid(10, 10);
 *     var tileset = new TileSet("/assets/terrain/tilesets/tilemap_color1.png", 64);
 *     var grass   = new TileSource(art, tileset, 1, 1);
 *
 *     grid.placeTile(8, 3, grass);
 *
 *     grid.getPlacedCount();              // 1
 *     grid.readTile(8, 3).isPresent();    // true
 *     grid.readTile(0, 0).isPresent();    // false
 *
 *     var placed = grid.readPlacedTiles().getFirst();
 *
 *     placed.row();                       // 8
 *     placed.column();                    // 3
 *     placed.tileset().path();            // "/assets/terrain/tilesets/tilemap_color1.png"
 *     placed.sourceRow();                 // 1
 *     placed.sourceColumn();              // 1
 * }
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public class TileGrid {

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private TileSource[][] tiles;
    private int rows;
    private int columns;
    private int placed;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a grid of the given size with every cell free.
     *
     * @param rows    the height in cells
     * @param columns the width in cells
     * @throws IllegalArgumentException if either count is zero or negative
     */
    public TileGrid(int rows, int columns) {
        requireSize(rows, columns);

        this.rows    = rows;
        this.columns = columns;
        this.tiles   = new TileSource[rows][columns];
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /** Returns the number of rows in this grid. */
    public int getRows() {
        return rows;
    }

    /** Returns the number of columns in this grid. */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the number of cells storing a tile.
     *
     * @return a count from 0 to {@code getRows() * getColumns()}
     */
    public int getPlacedCount() {
        return placed;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns whether every cell of this grid is free.
     *
     * @return {@code true} where {@link #getPlacedCount()} returns 0
     */
    public boolean isEmpty() {
        return placed == 0;
    }

    /**
     * Returns whether a row and column pair addresses a cell in this grid.
     *
     * @param row    the row to test
     * @param column the column to test
     * @return {@code true} where {@code row} is at least 0 and below {@link #getRows()}, and {@code column} is
     *         at least 0 and below {@link #getColumns()}
     */
    public boolean contains(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }

    /**
     * Returns the tile stored in one cell.
     *
     * @param row    the row to read
     * @param column the column to read
     * @return the {@link TileSource} in that cell, and empty for a free cell or for a position outside this grid
     */
    public Optional<TileSource> readTile(int row, int column) {
        return contains(row, column) ? Optional.ofNullable(tiles[row][column]) : Optional.empty();
    }

    /**
     * Returns one {@link PlacedTile} for each cell storing a tile, ordered by ascending row, and by ascending
     * column within a row.
     *
     * @return a new list, empty for a grid where every cell is free
     */
    public List<PlacedTile> readPlacedTiles() {
        var found = new ArrayList<PlacedTile>(placed);

        for (var row = 0; row < rows; row++) {
            for (var column = 0; column < columns; column++) {
                var tile = tiles[row][column];

                if (tile != null) {
                    var foundTile = new PlacedTile(
                            row, column, tile.tileset(), tile.sourceRow(), tile.sourceColumn()
                    );
                    found.add(foundTile);
                }
            }
        }

        return found;
    }

    /**
     * Counts the tiles standing outside the given bounds, which is the number that {@link #resize(int, int)}
     * would drop at that size.
     *
     * @param rows    the height to test, in cells
     * @param columns the width to test, in cells
     * @return the number of tiles standing at or beyond that row or that column, and 0 for a size that keeps
     *         every tile
     */
    public int countTilesOutside(int rows, int columns) {
        var outside = 0;

        for (var row = 0; row < this.rows; row++) {
            for (var column = 0; column < this.columns; column++) {
                if ((row >= rows || column >= columns) && tiles[row][column] != null) {
                    outside++;
                }
            }
        }

        return outside;
    }

    /**
     * Stores a tile in one cell, replacing any tile already stored there.
     *
     * @param row    the row to write
     * @param column the column to write
     * @param tile   the tile to store
     * @throws IndexOutOfBoundsException if the position lies outside this grid
     * @throws IllegalArgumentException  if the tile is null
     */
    public void placeTile(int row, int column, TileSource tile) {
        requireInside(row, column);

        if (tile == null) {
            throw new IllegalArgumentException("A placed tile requires a tile source");
        }

        if (tiles[row][column] == null) {
            placed++;
        }

        tiles[row][column] = tile;
    }

    /**
     * Frees one cell.
     *
     * @param row    the row to free
     * @param column the column to free
     * @throws IndexOutOfBoundsException if the position lies outside this grid
     */
    public void removeTile(int row, int column) {
        requireInside(row, column);

        if (tiles[row][column] != null) {
            placed--;
        }

        tiles[row][column] = null;
    }

    /** Frees every cell, so {@link #getPlacedCount()} returns 0. */
    public void clear() {
        tiles  = new TileSource[rows][columns];
        placed = 0;
    }

    /**
     * Sets the row and column count, keeping every tile that stands inside both the old bounds and the new ones
     * and dropping the rest. A caller that must keep every tile reads {@link #countTilesOutside(int, int)} first.
     *
     * @param rows    the new height in cells
     * @param columns the new width in cells
     * @throws IllegalArgumentException if either count is zero or negative
     */
    public void resize(int rows, int columns) {
        requireSize(rows, columns);

        var resized = new TileSource[rows][columns];
        var kept    = 0;

        for (var row = 0; row < Math.min(rows, this.rows); row++) {
            for (var column = 0; column < Math.min(columns, this.columns); column++) {
                resized[row][column] = tiles[row][column];

                if (resized[row][column] != null) {
                    kept++;
                }
            }
        }

        this.rows    = rows;
        this.columns = columns;
        this.tiles   = resized;
        this.placed  = kept;
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private static void requireSize(int rows, int columns) {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException(String.format("A grid size must be positive, given %d rows by %d columns", rows, columns));
        }
    }

    private void requireInside(int row, int column) {
        if (!contains(row, column)) {
            throw new IndexOutOfBoundsException(String.format("A grid of %d rows by %d columns has no cell at row %d, column %d", rows, columns, row, column));
        }
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * One tile placed on a grid, given as its position on the grid, the tileset that its art was cut from, and
     * its position inside that tileset.
     *
     * @param row          the row of the grid that this tile occupies
     * @param column       the column of the grid that this tile occupies
     * @param tileset      the tileset that this tile was cut from
     * @param sourceRow    the row of that tileset that this tile was cut from
     * @param sourceColumn the column of that tileset that this tile was cut from
     */
    public record PlacedTile(int row, int column, TileSet tileset, int sourceRow, int sourceColumn) {
    }

}
