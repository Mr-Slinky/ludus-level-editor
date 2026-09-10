package com.slinky.ludus.editor.data;

import java.awt.image.BufferedImage;

/**
 * The image that one cell draws, together with the {@link TileSet} that it was cut from and the cell that it
 * occupies inside that tileset.
 * <p>
 * <b>Describing the art of one cell</b>
 * <p>
 * A caller cuts one tile out of a tileset and reads its source cell back:
 * <pre>{@code
 * var tileset = new TileSet("/assets/terrain/tilesets/tilemap_color1.png", 64);
 * var grass   = new TileSource(art, tileset, 1, 1);
 *
 * grass.tileset().path();  // "/assets/terrain/tilesets/tilemap_color1.png"
 * grass.sourceRow();       // 1
 * }</pre>
 *
 * @param image        the pixels to draw, at the tileset's own resolution
 * @param tileset      the tileset that this tile was cut from
 * @param sourceRow    the row of that tileset that this tile was cut from
 * @param sourceColumn the column of that tileset that this tile was cut from
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public record TileSource(BufferedImage image, TileSet tileset, int sourceRow, int sourceColumn) {

    /**
     * Validates the image, the tileset and the two indices.
     *
     * @throws IllegalArgumentException if the image or the tileset is null, or if either index is negative
     */
    public TileSource {
        if (image == null) {
            throw new IllegalArgumentException("A tile source requires an image");
        }

        if (tileset == null) {
            throw new IllegalArgumentException("A tile source requires a tileset");
        }

        if (sourceRow < 0 || sourceColumn < 0) {
            throw new IllegalArgumentException(String.format("A source position must be 0 or greater, given row %d, column %d", sourceRow, sourceColumn));
        }
    }

}
