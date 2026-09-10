package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.data.TileData;

import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;

import javax.swing.ToolTipManager;

/**
 * A {@link Swatch} of metadata blocks, one per {@link Stamp}, laid out in a single row in the order that
 * {@link Stamp} declares its constants. Each block is a rounded square with the stamp's two letter code at its
 * centre.
 * <p>
 * A press selects one block, and {@link #readSelectedMetadata()} returns the {@link TileData} that the selected
 * block stamps. While the pointer rests over a block, the swatch shows that block's description as a tooltip.
 * <p>
 * <b>Reading the block that a press selects</b>
 * <p>
 * A caller builds a swatch of 64 pixel blocks, and reads the selection back after each press:
 * <pre>{@code
 * var metadata = new MetadataSwatch(64);
 *
 * // after a press on the "TR" block
 * metadata.getSelection();                                // Optional[java.awt.Point[x=0,y=0]]
 * metadata.readSelectedMetadata().get().isTraversable();  // true
 *
 * // after a second press on that same block
 * metadata.readSelectedMetadata();                        // Optional.empty
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *         <p>
 *         Last modified: 2026-09-10
 * @since 1.0.0
 */
public class MetadataSwatch extends Swatch {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color BLOCK_FILL    = Palette.blend(Palette.getActive().getDark(), Palette.getActive().getLight(), 0.15f);
    private static final Color LETTER_COLOUR = Palette.getActive().getLight();

    /** The gap in pixels between the rounded square of a block and the edge of its cell. */
    private static final int BLOCK_INSET = 6;
    private static final int BLOCK_ARC   = 12;
    private static final int LETTER_SIZE = 22;

    /**
     * Draws one block per {@link Stamp}, left to right in declaration order, each with its two letters centred.
     */
    private static BufferedImage renderBlocks(int size) {
        var stamps = Stamp.values();
        var image  = new BufferedImage(size * stamps.length, size, BufferedImage.TYPE_INT_ARGB);
        var canvas = image.createGraphics();

        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, LETTER_SIZE));

        var metrics = canvas.getFontMetrics();
        var block   = size - 2 * BLOCK_INSET;

        for (var index = 0; index < stamps.length; index++) {
            var left = index * size;
            var code = stamps[index].getCode();

            canvas.setColor(BLOCK_FILL);
            canvas.fillRoundRect(left + BLOCK_INSET, BLOCK_INSET, block, block, BLOCK_ARC, BLOCK_ARC);

            var x = left + (size - metrics.stringWidth(code)) / 2;
            var y = (size - metrics.getHeight()) / 2 + metrics.getAscent();

            canvas.setColor(LETTER_COLOUR);
            canvas.drawString(code, x, y);
        }

        canvas.dispose();

        return image;
    }

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a swatch of square blocks, one per {@link Stamp}.
     *
     * @param size the width and height of one block, in pixels
     * @throws IllegalArgumentException if the size is zero or negative
     */
    public MetadataSwatch(int size) {
        super(renderBlocks(size), size);

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns the metadata that the selected block stamps.
     *
     * @return the selected block's {@link TileData}, and empty while no block is selected
     */
    public Optional<TileData> readSelectedMetadata() {
        return getSelection().map(block -> Stamp.values()[block.x].getData());
    }

    /**
     * Returns the description of the block under the pointer, which Swing shows as the tooltip.
     *
     * @param e the mouse event that locates the pointer
     * @return the {@link Stamp#getDescription()} of that block, and null while the pointer is outside every block
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        return findStamp(e.getX(), e.getY()).map(Stamp::getDescription)
                                            .orElse(null);
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Returns the stamp whose block contains the given pixel, and empty for a pixel outside every block.
     */
    private Optional<Stamp> findStamp(int x, int y) {
        var stamps = Stamp.values();
        var column = x / getTileWidth();

        if (x < 0 || y < 0 || y >= getTileHeight() || column >= stamps.length) {
            return Optional.empty();
        }

        return Optional.of(stamps[column]);
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * One block on a {@link MetadataSwatch}. Each constant defines the two letter code drawn on its block, the
     * description shown as the block's tooltip, and the {@link TileData} that the block stamps.
     */
    public enum Stamp {

        /** Marks a tile as one that a player can walk over. */
        TRAVERSABLE("TR", "Traversable: a player can walk over this tile", new TileData(true));

        private final String   code;
        private final String   description;
        private final TileData data;

        Stamp(String code, String description, TileData data) {
            this.code        = code;
            this.description = description;
            this.data        = data;
        }

        /** Returns the two letters drawn on this block. */
        public String getCode() {
            return code;
        }

        /** Returns the text that the swatch shows as this block's tooltip. */
        public String getDescription() {
            return description;
        }

        /** Returns the metadata that this block stamps. */
        public TileData getData() {
            return data;
        }
    }

}
