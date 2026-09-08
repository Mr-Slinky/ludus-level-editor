package com.slinky.ludus.editor.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The tiles of one layer of a level, addressed by column and row. Every occupied cell stores one
 * {@link TileSource}, which records where the art was cut from as well as the pixels to draw.
 * <p>
 * Every method below takes a position as a column index followed by a row index. Both indices count from 0, and
 * {@link #contains(int, int)} returns whether a pair addresses a cell of this grid.
 * <p>
 * A caller sets a new size through {@link #resize(int, int)}, which keeps every tile inside both the old bounds
 * and the new ones and drops the rest. {@link #countTilesOutside(int, int)} returns how many tiles a given size
 * would drop, so a caller tests a size before applying it.
 * <p>
 * {@link #readPlacedTiles()} returns one {@link PlacedTile} for each occupied cell. Each of those states the
 * position of one tile on the grid alongside the tileset position its art was cut from.
 * <p>
 * <b>Stamping a tile and reading the layer back</b>
 * <p>
 * A caller places one tile on a new grid, then reads the cells back:
 * <pre>{@code
 * void stampGrass(BufferedImage art) {
 *     var grid  = new TileGrid(10, 10);
 *     var grass = new TileSource(art, 0, 1, 1);
 *
 *     grid.placeTile(3, 8, grass);
 *
 *     grid.getPlacedCount();              // 1
 *     grid.readTile(3, 8).isPresent();    // true
 *     grid.readTile(0, 0).isPresent();    // false
 *
 *     // [PlacedTile[column=3, row=8, tileset=0, sourceColumn=1, sourceRow=1]]
 *     grid.readPlacedTiles();
 * }
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class TileGrid {

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private TileSource[][] tiles;
    private int columns;
    private int rows;
    private int placed;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a grid of the given size with every cell free.
     *
     * @param columns the width in cells
     * @param rows    the height in cells
     * @throws IllegalArgumentException if either count is zero or negative
     */
    public TileGrid(int columns, int rows) {
        requireSize(columns, rows);

        this.columns = columns;
        this.rows    = rows;
        this.tiles   = new TileSource[rows][columns];
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /** Returns the number of columns in this grid. */
    public int getColumns() {
        return columns;
    }

    /** Returns the number of rows in this grid. */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the number of cells storing a tile.
     *
     * @return a count from 0 to {@code getColumns() * getRows()}
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
     * Returns whether a column and row pair addresses a cell in this grid.
     *
     * @param column the column to test
     * @param row    the row to test
     * @return {@code true} where {@code column} is at least 0 and below {@link #getColumns()}, and {@code row}
     *         is at least 0 and below {@link #getRows()}
     */
    public boolean contains(int column, int row) {
        return column >= 0 && column < columns && row >= 0 && row < rows;
    }

    /**
     * Returns the tile stored in one cell.
     *
     * @param column the column to read
     * @param row    the row to read
     * @return the {@link TileSource} in that cell, and empty for a free cell or for a position outside this grid
     */
    public Optional<TileSource> readTile(int column, int row) {
        return contains(column, row) ? Optional.ofNullable(tiles[row][column]) : Optional.empty();
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
                            column, row, tile.tileset(), tile.sourceColumn(), tile.sourceRow()
                    );
                    found.add(foundTile);
                }
            }
        }

        return found;
    }

    /**
     * Counts the tiles standing outside the given bounds, which is the number {@link #resize(int, int)} would
     * drop at that size.
     *
     * @param columns the width to test, in cells
     * @param rows    the height to test, in cells
     * @return the number of tiles standing at or beyond that column or that row, and 0 for a size that keeps
     *         every tile
     */
    public int countTilesOutside(int columns, int rows) {
        var outside = 0;

        for (var row = 0; row < this.rows; row++) {
            for (var column = 0; column < this.columns; column++) {
                if ((column >= columns || row >= rows) && tiles[row][column] != null) {
                    outside++;
                }
            }
        }

        return outside;
    }

    /**
     * Stores a tile in one cell, replacing any tile already stored there.
     *
     * @param column the column to write
     * @param row    the row to write
     * @param tile   the tile to store
     * @throws IndexOutOfBoundsException if the position lies outside this grid
     * @throws IllegalArgumentException  if the tile is null
     */
    public void placeTile(int column, int row, TileSource tile) {
        requireInside(column, row);

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
     * @param column the column to free
     * @param row    the row to free
     * @throws IndexOutOfBoundsException if the position lies outside this grid
     */
    public void removeTile(int column, int row) {
        requireInside(column, row);

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
     * Sets the column and row count, keeping every tile that stands inside both the old bounds and the new ones
     * and dropping the rest. A caller that must keep every tile reads {@link #countTilesOutside(int, int)} first.
     *
     * @param columns the new width in cells
     * @param rows    the new height in cells
     * @throws IllegalArgumentException if either count is zero or negative
     */
    public void resize(int columns, int rows) {
        requireSize(columns, rows);

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

        this.columns = columns;
        this.rows    = rows;
        this.tiles   = resized;
        this.placed  = kept;
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private static void requireSize(int columns, int rows) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException(String.format("A grid size must be positive, given %d by %d", columns, rows));
        }
    }

    private void requireInside(int column, int row) {
        if (contains(column, row) == false) {
            throw new IndexOutOfBoundsException(String.format("A grid of %d by %d has no cell at column %d, row %d", columns, rows, column, row));
        }
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * One tile placed on a grid, given as its position on the grid, the index of the tileset its art was cut
     * from, and its position inside that tileset.
     *
     * @param column       the column of the grid this tile occupies
     * @param row          the row of the grid this tile occupies
     * @param tileset      the index of the tileset this tile was cut from
     * @param sourceColumn the column of that tileset this tile was cut from
     * @param sourceRow    the row of that tileset this tile was cut from
     */
    public record PlacedTile(int column, int row, int tileset, int sourceColumn, int sourceRow) {
    }

}
