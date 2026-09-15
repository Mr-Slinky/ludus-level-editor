package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.Swatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the rule that one selection stands across both swatches, which is what keeps a stamp unambiguous: the
 * canvas is armed with a tile, or with metadata, or with nothing at all.
 * <p>
 * Each test presses a real {@link MouseEvent} onto one of the two swatches that a {@link RootPanel} builds, then
 * reads the canvas that the same panel wired to them. The panel needs no display for this, since nothing here
 * paints.
 * <p>
 * Blocks and tiles are both {@value RootPanel#CELL_SIZE} pixels square, so a press at {@code (10, 10)} lands on
 * the first of either.
 *
 * @author Claude Code
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-10
 * @since 1.0.0
 */
class RootPanelTest {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final String[] TILESETS = {
            "terrain/tilesets/tilemap_color1.png",
            "terrain/tilesets/tilemap_color2.png"
    };

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private RootPanel root;

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    @BeforeEach
    void buildRootPanel() {
        root = new RootPanel(TILESETS);
    }

    @Test
    @DisplayName("Picking a metadata block clears the tile that was picked, and arms the metadata alone")
    void testArmCanvasOnMetadataSelection_withATileAlreadyPicked_ArmsTheMetadataAlone() {
        pressTile(10, 10);
        pressMetadata(10, 10);

        var canvas = root.getLevelCanvas();

        assertAll(
                () -> assertTrue(canvas.getArmedMetadata().isPresent()),
                () -> assertTrue(canvas.getArmedTile().isEmpty()),
                () -> assertFalse(root.getSwatchPanel().hasSelection()),
                () -> assertTrue(root.getMetadataSwatch().hasSelection())
        );
    }

    @Test
    @DisplayName("Picking a tile clears the metadata block that was picked, and arms the tile alone")
    void testArmCanvasOnSelection_withAMetadataBlockAlreadyPicked_ArmsTheTileAlone() {
        pressMetadata(10, 10);
        pressTile(10, 10);

        var canvas = root.getLevelCanvas();

        assertAll(
                () -> assertTrue(canvas.getArmedTile().isPresent()),
                () -> assertTrue(canvas.getArmedMetadata().isEmpty()),
                () -> assertTrue(root.getSwatchPanel().hasSelection()),
                () -> assertFalse(root.getMetadataSwatch().hasSelection())
        );
    }

    @Test
    @DisplayName("Pressing the picked tile a second time disarms the canvas")
    void testArmCanvasOnSelection_withTwoPressesOnOneTile_DisarmsTheCanvas() {
        pressTile(10, 10);
        pressTile(20, 20);

        var canvas = root.getLevelCanvas();

        assertAll(
                () -> assertTrue(canvas.getArmedTile().isEmpty()),
                () -> assertTrue(canvas.getArmedMetadata().isEmpty()),
                () -> assertFalse(root.getSwatchPanel().hasSelection())
        );
    }

    @Test
    @DisplayName("Pressing the picked metadata block a second time disarms the canvas")
    void testArmCanvasOnMetadataSelection_withTwoPressesOnOneBlock_DisarmsTheCanvas() {
        pressMetadata(10, 10);
        pressMetadata(20, 20);

        var canvas = root.getLevelCanvas();

        assertAll(
                () -> assertTrue(canvas.getArmedTile().isEmpty()),
                () -> assertTrue(canvas.getArmedMetadata().isEmpty()),
                () -> assertFalse(root.getMetadataSwatch().hasSelection())
        );
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Presses the tileset on show in the deck, at a point in that swatch's own pixels.
     */
    private void pressTile(int x, int y) {
        pressOn(root.getSwatchPanel().getVisibleSwatch(), x, y);
    }

    /**
     * Presses the metadata swatch, at a point in its own pixels.
     */
    private void pressMetadata(int x, int y) {
        pressOn(root.getMetadataSwatch(), x, y);
    }

    private static void pressOn(Swatch target, int x, int y) {
        target.dispatchEvent(new MouseEvent(
                target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false
        ));
    }

}
