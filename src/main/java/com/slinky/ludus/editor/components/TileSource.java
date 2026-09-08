package com.slinky.ludus.editor.components;

import java.awt.image.BufferedImage;

/**
 * The image that one cell draws, together with the index of the tileset that it was cut from and the cell that
 * it occupies inside that tileset.
 *
 * @param image        the pixels to draw, at the tileset's own resolution
 * @param tileset      the index of the tileset that this tile was cut from
 * @param sourceRow    the row of that tileset that this tile was cut from
 * @param sourceColumn the column of that tileset that this tile was cut from
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public record TileSource(BufferedImage image, int tileset, int sourceRow, int sourceColumn) {

    /**
     * Validates the image and the three indices.
     *
     * @throws IllegalArgumentException if the image is null, or if any index is negative
     */
    public TileSource {
        if (image == null) {
            throw new IllegalArgumentException("A tile source requires an image");
        }

        if (tileset < 0) {
            throw new IllegalArgumentException(String.format("A tileset index must be 0 or greater, given %d", tileset));
        }

        if (sourceRow < 0 || sourceColumn < 0) {
            throw new IllegalArgumentException(String.format("A source position must be 0 or greater, given row %d, column %d", sourceRow, sourceColumn));
        }
    }

}
