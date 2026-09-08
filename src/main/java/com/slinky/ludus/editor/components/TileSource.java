package com.slinky.ludus.editor.components;

import java.awt.image.BufferedImage;

/**
 * The image one cell draws, together with the index of the tileset it was cut from and the cell of that
 * tileset it occupies.
 *
 * @param image        the pixels to draw, at the tileset's own resolution
 * @param tileset      the index of the tileset this tile was cut from
 * @param sourceColumn the column of that tileset this tile was cut from
 * @param sourceRow    the row of that tileset this tile was cut from
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public record TileSource(BufferedImage image, int tileset, int sourceColumn, int sourceRow) {

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

        if (sourceColumn < 0 || sourceRow < 0) {
            throw new IllegalArgumentException(String.format("A source position must be 0 or greater, given column %d, row %d", sourceColumn, sourceRow));
        }
    }

}
