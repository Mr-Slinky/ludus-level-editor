package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.TileSet;
import com.slinky.ludus.editor.data.TileSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the stamping that turns a press into a placed tile, which is the step that fills the grids a level is
 * written from.
 * <p>
 * Each test dispatches a real {@link MouseEvent} at the canvas and then reads the layer that came out, so the
 * conversion from a pixel position to a cell runs inside the build. A canvas needs no display for this: it
 * loads its water colour from the classpath and installs listeners, and it paints nothing until something asks
 * it to.
 * <p>
 * Cells are {@value #CELL_SIZE} pixels square here, so a press at {@code (x, y)} lands in row
 * {@code y / CELL_SIZE} and column {@code x / CELL_SIZE}.
 *
 * @author Claude Code
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
class LevelCanvasTest {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final int CELL_SIZE = 64;
    private static final int MAX_ROWS  = 15;
    private static final int MAX_COLS  = 15;

    private static final TileSet TILESET = new TileSet("/assets/terrain/tilesets/tilemap_color1.png", CELL_SIZE);

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private LevelCanvas canvas;
    private TileSource  tile;

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    @BeforeEach
    void buildCanvas() {
        canvas = new LevelCanvas(CELL_SIZE, MAX_ROWS, MAX_COLS);
        canvas.resizeGrid(10, 10);
        tile = new TileSource(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), TILESET, 5, 6);
    }

    @Test
    @DisplayName("A press stamps the armed tile into the cell under the pointer")
    void testStampAt_withArmedTile_PlacesItInTheCellUnderThePointer() {
        canvas.setArmedTile(tile);

        pressAt(9 * CELL_SIZE + 10, 6 * CELL_SIZE + 10);

        var placed = canvas.getLayer(0).readTile(6, 9);

        assertAll(
                () -> assertTrue(placed.isPresent()),
                () -> assertEquals(TILESET, placed.get().tileset()),
                () -> assertEquals(5, placed.get().sourceRow()),
                () -> assertEquals(6, placed.get().sourceColumn())
        );
    }

    @Test
    @DisplayName("A press with nothing armed leaves every layer free")
    void testStampAt_withNoArmedTile_LeavesEveryLayerFree() {
        pressAt(3 * CELL_SIZE, 3 * CELL_SIZE);

        assertEquals(0, canvas.countPlacedTiles());
    }

    @ParameterizedTest
    @CsvSource({"-5, 100", "100, -5", "700, 100", "100, 700"})
    @DisplayName("A press outside the grid leaves every layer free")
    void testStampAt_withPointOutsideTheGrid_LeavesEveryLayerFree(int x, int y) {
        canvas.setArmedTile(tile);

        pressAt(x, y);

        assertEquals(0, canvas.countPlacedTiles());
    }

    @Test
    @DisplayName("A press writes to the active layer and leaves the others free")
    void testSetActiveLayer_withSecondLayer_PlacesTheStampThere() {
        canvas.setArmedTile(tile);
        canvas.setActiveLayer(3);

        pressAt(2 * CELL_SIZE, 2 * CELL_SIZE);

        assertAll(
                () -> assertTrue(canvas.getLayer(3).readTile(2, 2).isPresent()),
                () -> assertTrue(canvas.getLayer(0).isEmpty()),
                () -> assertEquals(1, canvas.countPlacedTiles())
        );
    }

    @Test
    @DisplayName("Two presses on one cell replace the tile rather than adding one")
    void testStampAt_withTwoPressesOnOneCell_ReplacesTheTile() {
        var replacement = new TileSource(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), TILESET, 1, 2);

        canvas.setArmedTile(tile);
        pressAt(CELL_SIZE, CELL_SIZE);

        canvas.setArmedTile(replacement);
        pressAt(CELL_SIZE, CELL_SIZE);

        assertAll(
                () -> assertEquals(1, canvas.countPlacedTiles()),
                () -> assertEquals(1, canvas.getLayer(0).readTile(1, 1).get().sourceRow())
        );
    }

    @Test
    @DisplayName("A grid grows and keeps every tile where it stands")
    void testResizeGrid_withLargerSize_KeepsEveryPlacedTile() {
        canvas.setArmedTile(tile);
        pressAt(2 * CELL_SIZE, 2 * CELL_SIZE);

        canvas.resizeGrid(12, 12);

        assertAll(
                () -> assertEquals(12, canvas.getRows()),
                () -> assertEquals(12, canvas.getColumns()),
                () -> assertTrue(canvas.getLayer(0).readTile(2, 2).isPresent())
        );
    }

    @Test
    @DisplayName("A shrink that would drop a tile is refused before it is applied")
    void testCanResizeTo_withSizeDroppingATile_ReturnsFalse() {
        canvas.setArmedTile(tile);
        canvas.setActiveLayer(2);
        pressAt(8 * CELL_SIZE, 8 * CELL_SIZE);

        assertAll(
                () -> assertFalse(canvas.canResizeTo(5, 5)),
                () -> assertTrue(canvas.canResizeTo(10, 10))
        );
    }

    @Test
    @DisplayName("A canvas takes the pixel size of its grid")
    void testGetPreferredSize_withResizedGrid_ReturnsGridPixelSize() {
        canvas.resizeGrid(8, 11);

        assertEquals(new Dimension(11 * CELL_SIZE, 8 * CELL_SIZE), canvas.getPreferredSize());
    }

    @Test
    @DisplayName("A canvas stacks six layers, so a level writes six")
    void testGetLayerCount_withNewCanvas_ReturnsMaxLayers() {
        assertEquals(LevelCanvas.MAX_LAYERS, canvas.getLayerCount());
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Dispatches a press at a point in component pixels, which is the gesture that stamps a tile.
     */
    private void pressAt(int x, int y) {
        canvas.dispatchEvent(new MouseEvent(
                canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false
        ));
    }

}
