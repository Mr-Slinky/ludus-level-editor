package com.slinky.ludus.editor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The tiles that make up one layer of a level, addressed by row and column. Every occupied cell stores one
 * {@link TileSource}, which records where the art was cut from as well as the pixels to draw.
 * <p>
 * Alongside its tile, every cell stores one {@link TileData}. {@link #placeMetadata(int, int, TileData)} writes
 * it, and {@link #readMetadata(int, int)} returns {@link TileData#DEFAULT} for a cell that has none.
 * {@link #removeTile(int, int)} and {@link #clear()} remove a cell's tile and its metadata together.
 * <p>
 * Every method below takes a position as a row index followed by a column index. Both indices count from 0, and
 * {@link #contains(int, int)} returns whether a pair addresses a cell of this grid.
 * <p>
 * A caller sets a new size through {@link #resize(int, int)}, which keeps the tile and the metadata of every
 * cell inside both the old bounds and the new ones, and drops the rest. {@link #countTilesOutside(int, int)}
 * returns how many tiles a given size would drop, so a caller tests a size before applying it.
 * <p>
 * {@link #readPlacedTiles()} returns one {@link PlacedTile} for each occupied cell. Each of those states where
 * one tile stands on the grid, the {@link TileSet} that its art was cut from, the cell that it occupies inside
 * that tileset, and the {@link TileData} stored in its cell.
 * <p>
 * <b>Stamping a tile and reading the layer back</b>
 * <p>
 * A caller places one tile and its metadata on a new grid, then reads the cells back:
 * <pre>{@code
 * void stampGrass(BufferedImage art) {
 *     var grid    = new TileGrid(10, 10);
 *     var tileset = new TileSet("/assets/terrain/tilesets/tilemap_color1.png", 64);
 *     var grass   = new TileSource(art, tileset, 1, 1);
 *
 *     grid.placeTile(8, 3, grass);
 *     grid.placeMetadata(8, 3, new TileData(true, false));
 *
 *     grid.getPlacedCount();                    // 1
 *     grid.readTile(8, 3).isPresent();          // true
 *     grid.readTile(0, 0).isPresent();          // false
 *     grid.readMetadata(0, 0).isTraversable();  // false
 *
 *     var placed = grid.readPlacedTiles().getFirst();
 *
 *     placed.row();                             // 8
 *     placed.column();                          // 3
 *     placed.tileset().path();                  // "/assets/terrain/tilesets/tilemap_color1.png"
 *     placed.sourceRow();                       // 1
 *     placed.sourceColumn();                    // 1
 *     placed.data().isTraversable();            // true
 * }
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-14
 * @since 1.0.0
 */
public class TileGrid {

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private TileSource[][] tiles;
    private TileData[][] metadata;
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

        this.rows     = rows;
        this.columns  = columns;
        this.tiles    = new TileSource[rows][columns];
        this.metadata = new TileData[rows][columns];
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
     * Returns the metadata stored in one cell.
     *
     * @param row    the row to read
     * @param column the column to read
     * @return the {@link TileData} last placed in that cell, and {@link TileData#DEFAULT} for a cell that has none
     * @throws IndexOutOfBoundsException if the position lies outside this grid
     */
    public TileData readMetadata(int row, int column) {
        requireInside(row, column);

        var stamped = metadata[row][column];

        return stamped == null ? TileData.DEFAULT : stamped;
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
                            row, column, tile.tileset(), tile.sourceRow(), tile.sourceColumn(),
                            readMetadata(row, column)
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

        if (tile == null) throw new IllegalArgumentException("A placed tile requires a tile source");

        if (tiles[row][column] == null) {
            placed++;
        }

        tiles[row][column] = tile;
    }

    /**
     * Stores metadata in one cell, replacing any metadata already stored there and leaving the cell's tile as it
     * stands.
     *
     * @param row    the row to write
     * @param column the column to write
     * @param data   the metadata to store
     * @throws IndexOutOfBoundsException if the position lies outside this grid
     * @throws IllegalArgumentException  if the data is null
     */
    public void placeMetadata(int row, int column, TileData data) {
        requireInside(row, column);

        if (data == null) {
            throw new IllegalArgumentException("A metadata stamp requires tile data");
        }

        metadata[row][column] = data;
    }

    /**
     * Frees one cell, removing its tile and its metadata together.
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

        tiles[row][column]    = null;
        metadata[row][column] = null;
    }

    /** Removes every tile and all metadata, so {@link #getPlacedCount()} returns 0. */
    public void clear() {
        tiles    = new TileSource[rows][columns];
        metadata = new TileData[rows][columns];
        placed   = 0;
    }

    /**
     * Sets the row and column count. Every cell inside both the old bounds and the new ones keeps its tile and
     * its metadata, and the grid drops every other cell. A caller that must keep every tile reads
     * {@link #countTilesOutside(int, int)} first.
     *
     * @param rows    the new height in cells
     * @param columns the new width in cells
     * @throws IllegalArgumentException if either count is zero or negative
     */
    public void resize(int rows, int columns) {
        requireSize(rows, columns);

        var resized  = new TileSource[rows][columns];
        var retained = new TileData[rows][columns];
        var kept     = 0;

        for (var row = 0; row < Math.min(rows, this.rows); row++) {
            for (var column = 0; column < Math.min(columns, this.columns); column++) {
                resized[row][column]  = tiles[row][column];
                retained[row][column] = metadata[row][column];

                if (resized[row][column] != null) {
                    kept++;
                }
            }
        }

        this.rows     = rows;
        this.columns  = columns;
        this.tiles    = resized;
        this.metadata = retained;
        this.placed   = kept;
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
     * One tile placed on a grid, given as its position on the grid, the tileset that its art was cut from, its
     * position inside that tileset, and the metadata stored in its cell.
     *
     * @param row          the row of the grid that this tile occupies
     * @param column       the column of the grid that this tile occupies
     * @param tileset      the tileset that this tile was cut from
     * @param sourceRow    the row of that tileset that this tile was cut from
     * @param sourceColumn the column of that tileset that this tile was cut from
     * @param data         the metadata stored in that cell, as {@link TileGrid#readMetadata(int, int)} returns it
     */
    public record PlacedTile(int row, int column, TileSet tileset, int sourceRow, int sourceColumn, TileData data) {
    }

}
