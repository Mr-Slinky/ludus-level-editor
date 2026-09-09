package com.slinky.ludus.editor.data;

/**
 * One tileset that a level draws its art from, given as the classpath resource that its image loads from and
 * the pixel size of a single cell.
 * <p>
 * The path starts at the classpath root and keeps the file extension. A tileset stored under the assets tree
 * therefore takes the form {@code /assets/terrain/tilesets/tilemap_color1.png}. Every cell is square, so
 * {@link #cellSize()} returns both its width and its height in pixels.
 * <p>
 * <b>Describing a tileset cut into 64 pixel cells</b>
 * <p>
 * A caller builds one and reads both components back:
 * <pre>{@code
 * var tileset = new TileSet("/assets/terrain/tilesets/tilemap_color1.png", 64);
 *
 * tileset.path();      // "/assets/terrain/tilesets/tilemap_color1.png"
 * tileset.cellSize();  // 64
 * }</pre>
 *
 * @param path     the classpath resource that the tileset image loads from, starting at the classpath root
 * @param cellSize the width and height in pixels of a single cell
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public record TileSet(String path, int cellSize) {

    /**
     * Validates the path and the cell size.
     *
     * @throws IllegalArgumentException if the path is null or blank, or if the cell size is below 1
     */
    public TileSet {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("A tileset requires a resource path");
        }

        if (cellSize < 1) {
            throw new IllegalArgumentException(String.format("A tileset cell size must be positive, given %d", cellSize));
        }
    }

}