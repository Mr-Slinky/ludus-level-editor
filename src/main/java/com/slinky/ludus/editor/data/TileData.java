package com.slinky.ludus.editor.data;

/**
 * The non-visual metadata of one placed tile. {@link TileSource} describes the art that a tile draws, and this
 * record describes how the tile behaves.
 * <p>
 * <b>Marking a tile as traversable</b>
 * <p>
 * A caller builds the metadata for a traversable tile, and reads it back beside the default:
 * <pre>{@code
 * var walkable = new TileData(true);
 *
 * walkable.isTraversable();           // true
 * TileData.DEFAULT.isTraversable();   // false
 * }</pre>
 *
 * @param isTraversable whether a player can walk over the tile
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *         <p>
 *         Last modified: 2026-09-10
 * @since 1.0.0
 */
public record TileData(boolean isTraversable) {

    /** The metadata of a tile that no stamp has reached. */
    public static final TileData DEFAULT = new TileData(false);

}
