package com.slinky.ludus.editor.data;

/**
 * The non-visual metadata of one placed tile. {@link TileSource} describes the art that a tile draws, and this
 * record describes how the tile behaves.
 * <p>
 * <b>Marking a tile as traversable</b>
 * <p>
 * A caller builds the metadata for a traversable tile, and reads it back beside the default:
 * <pre>{@code
 * var walkable = new TileData(true, false);
 *
 * walkable.isTraversable();           // true
 * walkable.hasShadow();               // false
 * TileData.DEFAULT.isTraversable();   // false
 * }</pre>
 * <p>
 * <b>Stamping a shadow onto a traversable tile</b>
 * <p>
 * A caller combines the metadata already in a cell with the metadata stamped over it, and the result keeps
 * every flag that either record sets:
 * <pre>{@code
 * var walkable = new TileData(true, false);
 * var shaded   = walkable.combine(new TileData(false, true));
 *
 * shaded.isTraversable();  // true
 * shaded.hasShadow();      // true
 * }</pre>
 *
 * @param isTraversable whether a player can walk over the tile
 * @param hasShadow     whether the game draws a shadow over the tile
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *         <p>
 *         Last modified: 2026-09-14
 * @since 1.0.0
 */
public record TileData(boolean isTraversable, boolean hasShadow) {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The metadata of a tile that no stamp has reached. */
    public static final TileData DEFAULT = new TileData(false, false);

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns the metadata that stamping the given metadata over this one produces. Each flag of the result is
     * {@code true} wherever this record or the stamped one sets it.
     *
     * @param stamped the metadata stamped over this one
     * @return a new {@code TileData}, equal to this one where the stamped metadata sets no flag
     * @throws IllegalArgumentException if the stamped metadata is null
     */
    public TileData combine(TileData stamped) {
        if (stamped == null) {
            throw new IllegalArgumentException("A combined stamp requires tile data");
        }

        return new TileData(isTraversable || stamped.isTraversable, hasShadow || stamped.hasShadow);
    }

}
