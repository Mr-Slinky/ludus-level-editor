package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.TileData;
import com.slinky.ludus.editor.data.TileSet;
import com.slinky.ludus.editor.data.TileSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two gestures that a user edits a level with: the press that selects a cell without altering it, and
 * the stamp that fills the grids a level is written from. The right press that frees a cell again goes in here
 * too.
 * <p>
 * Each test dispatches a real {@link MouseEvent} at the canvas and then reads the layer that came out, so the
 * conversion from a pixel position to a cell runs inside the build. A canvas needs no display for this: it
 * loads its water colour from the classpath and installs listeners, and it paints nothing until something asks
 * it to.
 * <p>
 * Stamping runs through {@link LevelCanvas#stampHoveredCell()}, which is what the E key is bound to. A test
 * moves the pointer and then calls it, which is the pair of steps that a user performs.
 * <p>
 * Cells are {@value #CELL_SIZE} pixels square here, so a point {@code (x, y)} lands in row
 * {@code y / CELL_SIZE} and column {@code x / CELL_SIZE}.
 * <p>
 * The shadow tests paint the canvas into an image off screen over opaque white tiles. A cell of that image
 * shows a shadow wherever any of its pixels differs from white.
 *
 * @author Claude Code
 * @version 2.0.0
 *          <p>
 *          Last modified: 2026-09-14
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
    @DisplayName("A press selects the cell under the pointer and stamps nothing")
    void testMousePressed_withArmedTile_SelectsTheCellAndLeavesEveryLayerFree() {
        canvas.setArmedTile(tile);

        pressAt(9 * CELL_SIZE + 10, 6 * CELL_SIZE + 10);

        assertAll(
                () -> assertEquals(new Point(9, 6), canvas.getSelectedCell().orElseThrow()),
                () -> assertEquals(0, canvas.countPlacedTiles()),
                () -> assertTrue(canvas.getArmedTile().isPresent())
        );
    }

    @Test
    @DisplayName("A press outside the grid selects no cell")
    void testMousePressed_withPointOutsideTheGrid_SelectsNoCell() {
        pressAt(11 * CELL_SIZE, 2 * CELL_SIZE);

        assertTrue(canvas.getSelectedCell().isEmpty());
    }

    @Test
    @DisplayName("A stamp writes the armed tile into the cell under the pointer")
    void testStampHoveredCell_withArmedTile_PlacesItInTheCellUnderThePointer() {
        canvas.setArmedTile(tile);

        stampAt(9 * CELL_SIZE + 10, 6 * CELL_SIZE + 10);

        var placed = canvas.getLayer(0).readTile(6, 9);

        assertAll(
                () -> assertTrue(placed.isPresent()),
                () -> assertEquals(TILESET, placed.get().tileset()),
                () -> assertEquals(5, placed.get().sourceRow()),
                () -> assertEquals(6, placed.get().sourceColumn())
        );
    }

    @Test
    @DisplayName("A stamp with nothing armed leaves every layer free")
    void testStampHoveredCell_withNothingArmed_LeavesEveryLayerFree() {
        stampAt(3 * CELL_SIZE, 3 * CELL_SIZE);

        assertEquals(0, canvas.countPlacedTiles());
    }

    @ParameterizedTest
    @CsvSource({"-5, 100", "100, -5", "700, 100", "100, 700"})
    @DisplayName("A stamp with the pointer away from the grid leaves every layer free")
    void testStampHoveredCell_withPointerOutsideTheGrid_LeavesEveryLayerFree(int x, int y) {
        canvas.setArmedTile(tile);

        stampAt(x, y);

        assertEquals(0, canvas.countPlacedTiles());
    }

    @Test
    @DisplayName("A stamp writes to the active layer and leaves the others free")
    void testSetActiveLayer_withSecondLayer_PlacesTheStampThere() {
        canvas.setArmedTile(tile);
        canvas.setActiveLayer(3);

        stampAt(2 * CELL_SIZE, 2 * CELL_SIZE);

        assertAll(
                () -> assertTrue(canvas.getLayer(3).readTile(2, 2).isPresent()),
                () -> assertTrue(canvas.getLayer(0).isEmpty()),
                () -> assertEquals(1, canvas.countPlacedTiles())
        );
    }

    @Test
    @DisplayName("Two stamps on one cell replace the tile rather than adding one")
    void testStampHoveredCell_withTwoStampsOnOneCell_ReplacesTheTile() {
        var replacement = new TileSource(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), TILESET, 1, 2);

        canvas.setArmedTile(tile);
        stampAt(CELL_SIZE, CELL_SIZE);

        canvas.setArmedTile(replacement);
        stampAt(CELL_SIZE, CELL_SIZE);

        assertAll(
                () -> assertEquals(1, canvas.countPlacedTiles()),
                () -> assertEquals(1, canvas.getLayer(0).readTile(1, 1).get().sourceRow())
        );
    }

    @Test
    @DisplayName("A stamp of armed metadata writes it onto the tile already in the cell")
    void testStampHoveredCell_withArmedMetadataOverAPlacedTile_WritesItOntoThatTile() {
        canvas.setArmedTile(tile);
        stampAt(4 * CELL_SIZE, 4 * CELL_SIZE);

        canvas.setArmedMetadata(new TileData(true, false));
        stampAt(4 * CELL_SIZE, 4 * CELL_SIZE);

        assertTrue(canvas.getLayer(0).readMetadata(4, 4).isTraversable());
    }

    @Test
    @DisplayName("A second metadata stamp on one tile keeps the flag that the first stamp set")
    void testStampHoveredCell_withTwoMetadataStampsOnOneTile_KeepsBothFlags() {
        canvas.setArmedTile(tile);
        stampAt(4 * CELL_SIZE, 4 * CELL_SIZE);

        canvas.setArmedMetadata(new TileData(true, false));
        stampAt(4 * CELL_SIZE, 4 * CELL_SIZE);

        canvas.setArmedMetadata(new TileData(false, true));
        stampAt(4 * CELL_SIZE, 4 * CELL_SIZE);

        assertEquals(new TileData(true, true), canvas.getLayer(0).readMetadata(4, 4));
    }

    @Test
    @DisplayName("A stamp of armed metadata over a free cell leaves that cell free")
    void testStampHoveredCell_withArmedMetadataOverAFreeCell_LeavesThatCellFree() {
        canvas.setArmedMetadata(new TileData(true, false));

        stampAt(4 * CELL_SIZE, 4 * CELL_SIZE);

        assertAll(
                () -> assertEquals(0, canvas.countPlacedTiles()),
                () -> assertFalse(canvas.getLayer(0).readMetadata(4, 4).isTraversable())
        );
    }

    @Test
    @DisplayName("A grid grows and keeps every tile where it stands")
    void testResizeGrid_withLargerSize_KeepsEveryPlacedTile() {
        canvas.setArmedTile(tile);
        stampAt(2 * CELL_SIZE, 2 * CELL_SIZE);

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
        stampAt(8 * CELL_SIZE, 8 * CELL_SIZE);

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

    @Test
    @DisplayName("A right press frees the cell under the pointer")
    void testEraseAt_withRightPressOverAPlacedTile_FreesThatCell() {
        canvas.setArmedTile(tile);
        stampAt(4 * CELL_SIZE + 10, 7 * CELL_SIZE + 10);

        rightPressAt(4 * CELL_SIZE + 20, 7 * CELL_SIZE + 20);

        assertAll(
                () -> assertFalse(canvas.getLayer(0).readTile(7, 4).isPresent()),
                () -> assertEquals(0, canvas.countPlacedTiles())
        );
    }

    @Test
    @DisplayName("A right press over a free cell leaves every layer free")
    void testEraseAt_withRightPressOverAFreeCell_LeavesEveryLayerFree() {
        rightPressAt(3 * CELL_SIZE, 3 * CELL_SIZE);

        assertEquals(0, canvas.countPlacedTiles());
    }

    @Test
    @DisplayName("A right press frees the active layer and leaves the tile below it")
    void testEraseAt_withOneTilePerLayer_FreesTheActiveLayerAlone() {
        canvas.setArmedTile(tile);
        stampAt(5 * CELL_SIZE, 5 * CELL_SIZE);

        canvas.setActiveLayer(2);
        stampAt(5 * CELL_SIZE, 5 * CELL_SIZE);

        rightPressAt(5 * CELL_SIZE, 5 * CELL_SIZE);

        assertAll(
                () -> assertFalse(canvas.getLayer(2).readTile(5, 5).isPresent()),
                () -> assertTrue(canvas.getLayer(0).readTile(5, 5).isPresent()),
                () -> assertEquals(1, canvas.countPlacedTiles())
        );
    }

    @ParameterizedTest
    @CsvSource({"-5, 100", "100, -5", "700, 100", "100, 700"})
    @DisplayName("A right press outside the grid keeps every placed tile")
    void testEraseAt_withPointOutsideTheGrid_KeepsEveryPlacedTile(int x, int y) {
        canvas.setArmedTile(tile);
        stampAt(2 * CELL_SIZE, 2 * CELL_SIZE);

        rightPressAt(x, y);

        assertEquals(1, canvas.countPlacedTiles());
    }

    @Test
    @DisplayName("A right press keeps the armed tile, so the next stamp lands again")
    void testEraseAt_withArmedTile_KeepsItArmed() {
        canvas.setArmedTile(tile);
        stampAt(CELL_SIZE, CELL_SIZE);

        rightPressAt(CELL_SIZE, CELL_SIZE);
        stampAt(CELL_SIZE, CELL_SIZE);

        assertAll(
                () -> assertTrue(canvas.getArmedTile().isPresent()),
                () -> assertTrue(canvas.getLayer(0).readTile(1, 1).isPresent())
        );
    }

    @ParameterizedTest
    @CsvSource({"true, true", "false, false"})
    @DisplayName("A shadow paints over the tile below it on the same layer, which the grid paints after it")
    void testPaintComponent_withTileBelowOnTheSameLayer_DarkensThatTileWhereTheFlagIsSet(boolean hasShadow, boolean expectDarkened) {
        var layer = canvas.getLayer(0);
        layer.placeTile(4, 4, buildWhiteTile());
        layer.placeMetadata(4, 4, new TileData(false, hasShadow));
        layer.placeTile(5, 4, buildWhiteTile());

        var painted = paintCanvas();

        assertEquals(expectDarkened, countPixelsOtherThanWhite(painted, 5, 4) > 0);
    }

    @Test
    @DisplayName("A tile on a higher layer covers the shadow on the layer below it")
    void testPaintComponent_withTileOnTheLayerAboveAShadow_CoversTheShadow() {
        canvas.getLayer(0).placeTile(4, 4, buildWhiteTile());
        canvas.getLayer(0).placeMetadata(4, 4, new TileData(false, true));
        canvas.getLayer(1).placeTile(4, 4, buildWhiteTile());

        var painted = paintCanvas();

        assertEquals(0, countPixelsOtherThanWhite(painted, 4, 4));
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Paints the canvas at its preferred size into an image off screen, which is how a test reads what a user
     * would see.
     */
    private BufferedImage paintCanvas() {
        canvas.setSize(canvas.getPreferredSize());

        var image    = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();

        canvas.printAll(graphics);
        graphics.dispose();

        return image;
    }

    /**
     * Builds a tile from one opaque white pixel, which the canvas stretches over a whole cell.
     */
    private static TileSource buildWhiteTile() {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, Color.WHITE.getRGB());

        return new TileSource(image, TILESET, 0, 0);
    }

    /**
     * Counts the pixels inside one cell of a painted canvas that are anything other than opaque white.
     */
    private static int countPixelsOtherThanWhite(BufferedImage painted, int row, int column) {
        var count = 0;

        // the grid line runs along the top and the left edge of every cell, so the count starts one pixel in
        for (var y = row * CELL_SIZE + 1; y < (row + 1) * CELL_SIZE; y++) {
            for (var x = column * CELL_SIZE + 1; x < (column + 1) * CELL_SIZE; x++) {
                if (painted.getRGB(x, y) != Color.WHITE.getRGB()) {
                    count++;
                }
            }
        }

        return count;
    }
    /**
     * Moves the pointer to a point in component pixels and stamps there, which is the pair of steps that a user
     * performs with the pointer and the E key.
     */
    private void stampAt(int x, int y) {
        moveTo(x, y);
        canvas.stampHoveredCell();
    }

    /**
     * Dispatches a move to a point in component pixels, which is what tells the canvas where the pointer rests.
     */
    private void moveTo(int x, int y) {
        canvas.dispatchEvent(new MouseEvent(
                canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false
        ));
    }

    /**
     * Dispatches a press at a point in component pixels, which is the gesture that selects a cell.
     */
    private void pressAt(int x, int y) {
        canvas.dispatchEvent(new MouseEvent(
                canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false
        ));
    }

    /**
     * Dispatches a press of button 3 at a point in component pixels, which is the gesture that frees a cell. The
     * modifier and the button number both go in, since {@link javax.swing.SwingUtilities#isRightMouseButton} reads
     * the extended modifiers.
     */
    private void rightPressAt(int x, int y) {
        canvas.dispatchEvent(new MouseEvent(
                canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK,
                x, y, 1, false, MouseEvent.BUTTON3
        ));
    }

}
