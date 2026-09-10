package com.slinky.ludus.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.panels.SwatchPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the whole path from a press to a level file, over the five real terrain tilesets.
 * <p>
 * Each test presses on a swatch to select a tile, arms the canvas with what the deck reports, and presses on
 * the canvas to stamp it, which is the sequence that {@code RootPanel} wires up. It then converts the canvas
 * through {@link LevelWriter#buildLevelJson(LevelCanvas)} and reads the JSON back.
 * <p>
 * The test that matters most here is
 * {@link #testBuildLevelJson_withTwoOfFiveTilesetsPainted_ReturnsThoseTwoAlone()}. A deck loads five tilesets,
 * a user paints from two, and the file lists those two, with each tile's index resolving against that list.
 *
 * @author Claude Code
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
class LevelWriterTest {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final int CELL_SIZE = 64;

    private static final String[] TILESETS = {
            "terrain/tilesets/tilemap_color1.png",
            "terrain/tilesets/tilemap_color2.png",
            "terrain/tilesets/tilemap_color3.png",
            "terrain/tilesets/tilemap_color4.png",
            "terrain/tilesets/tilemap_color5.png"
    };

    private static final String SECOND_PATH = "/assets/terrain/tilesets/tilemap_color2.png";
    private static final String THIRD_PATH  = "/assets/terrain/tilesets/tilemap_color3.png";

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private SwatchPanel deck;
    private LevelCanvas canvas;

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    @BeforeEach
    void buildEditor() {
        deck   = new SwatchPanel(CELL_SIZE, TILESETS);
        canvas = new LevelCanvas(CELL_SIZE, 15, 15);
        canvas.resizeGrid(10, 11);
    }

    @Test
    @DisplayName("A level lists the tilesets it draws from, rather than every tileset loaded")
    void testBuildLevelJson_withTwoOfFiveTilesetsPainted_ReturnsThoseTwoAlone() {
        stamp(1, 3, 5, 0, 4, 4);
        stamp(2, 5, 6, 1, 6, 9);

        var tilesets = readLevel().getAsJsonArray("tilesets");

        assertAll(
                () -> assertEquals(2, tilesets.size()),
                () -> assertEquals(SECOND_PATH, readPath(tilesets, 0)),
                () -> assertEquals(THIRD_PATH,  readPath(tilesets, 1)),
                () -> assertEquals(CELL_SIZE, tilesets.get(0).getAsJsonObject().get("cellSize").getAsInt())
        );
    }

    @Test
    @DisplayName("A tile resolves back to the tileset that a user selected it from")
    void testBuildLevelJson_withTilesFromTwoTilesets_ReturnsAnIndexResolvingToEachPath() {
        stamp(1, 3, 5, 0, 4, 4);
        stamp(2, 5, 6, 1, 6, 9);

        var level  = readLevel();
        var bottom = readTiles(level, 0).get(0).getAsJsonObject();
        var upper  = readTiles(level, 1).get(0).getAsJsonObject();

        assertAll(
                () -> assertEquals(SECOND_PATH, resolveTilesetPath(level, bottom)),
                () -> assertEquals(THIRD_PATH,  resolveTilesetPath(level, upper))
        );
    }

    @Test
    @DisplayName("A stamped tile states where it stands and which cell it was cut from")
    void testBuildLevelJson_withOneStampedTile_ReturnsItsPositionAndSourceCell() {
        stamp(2, 5, 6, 0, 6, 9);

        var tile = readTiles(readLevel(), 0).get(0).getAsJsonObject();

        assertAll(
                () -> assertEquals(6, tile.get("row").getAsInt()),
                () -> assertEquals(9, tile.get("column").getAsInt()),
                () -> assertEquals(5, tile.get("sourceRow").getAsInt()),
                () -> assertEquals(6, tile.get("sourceColumn").getAsInt())
        );
    }

    @Test
    @DisplayName("A level states the size that the canvas was resized to")
    void testBuildLevelJson_withResizedCanvas_ReturnsThatSize() {
        var level = readLevel();

        assertAll(
                () -> assertEquals(10, level.get("rows").getAsInt()),
                () -> assertEquals(11, level.get("columns").getAsInt())
        );
    }

    @Test
    @DisplayName("An unpainted canvas writes a level with no tilesets and six free layers")
    void testBuildLevelJson_withEmptyCanvas_ReturnsSixFreeLayers() {
        var level  = readLevel();
        var layers = level.getAsJsonArray("layers");

        assertAll(
                () -> assertTrue(level.getAsJsonArray("tilesets").isEmpty()),
                () -> assertEquals(LevelCanvas.MAX_LAYERS, layers.size()),
                () -> assertTrue(readTiles(level, 0).isEmpty()),
                () -> assertTrue(readTiles(level, 5).isEmpty())
        );
    }

    @Test
    @DisplayName("A layer that a user skipped writes an entry, so the layers stay aligned")
    void testBuildLevelJson_withPaintOnLayerThree_ReturnsThatTileInTheFourthEntry() {
        stamp(0, 0, 0, 3, 2, 2);

        var level = readLevel();

        assertAll(
                () -> assertEquals(LevelCanvas.MAX_LAYERS, level.getAsJsonArray("layers").size()),
                () -> assertTrue(readTiles(level, 0).isEmpty()),
                () -> assertTrue(readTiles(level, 2).isEmpty()),
                () -> assertEquals(1, readTiles(level, 3).size()),
                () -> assertTrue(readTiles(level, 4).isEmpty())
        );
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Performs one whole editing gesture: flip the deck to a tileset, press the tile at the given source cell,
     * arm the canvas with what the deck reports, and press the cell of the given layer that takes the stamp.
     *
     * @param tileset      the position in the deck to flip to
     * @param sourceRow    the row of that tileset to press
     * @param sourceColumn the column of that tileset to press
     * @param layer        the canvas layer that takes the stamp
     * @param row          the canvas row to press
     * @param column       the canvas column to press
     */
    private void stamp(int tileset, int sourceRow, int sourceColumn, int layer, int row, int column) {
        deck.showSwatch(tileset);
        press(deck.getVisibleSwatch(), sourceColumn * CELL_SIZE, sourceRow * CELL_SIZE);

        canvas.setArmedTile(deck.readSelectedTile().orElseThrow());
        canvas.setActiveLayer(layer);
        press(canvas, column * CELL_SIZE, row * CELL_SIZE);
    }

    private static void press(Component target, int x, int y) {
        target.dispatchEvent(new MouseEvent(
                target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false
        ));
    }

    /**
     * Converts the canvas and parses the result, so every assertion reads the text that a level file holds
     * rather than the object it was built from.
     */
    private JsonObject readLevel() {
        return JsonParser.parseString(LevelWriter.buildLevelJson(canvas)).getAsJsonObject();
    }

    private static JsonArray readTiles(JsonObject level, int layer) {
        return level.getAsJsonArray("layers").get(layer).getAsJsonObject().getAsJsonArray("tiles");
    }

    private static String readPath(JsonArray tilesets, int position) {
        return tilesets.get(position).getAsJsonObject().get("path").getAsString();
    }

    /**
     * Resolves a tile's {@code tileset} index against the level's own {@code tilesets} array, which is the step
     * that Ludus performs when it loads a level.
     */
    private static String resolveTilesetPath(JsonObject level, JsonObject tile) {
        return readPath(level.getAsJsonArray("tilesets"), tile.get("tileset").getAsInt());
    }

}
