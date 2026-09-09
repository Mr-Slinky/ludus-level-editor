package com.slinky.ludus.editor.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the level format that {@link JsonUtil} writes, which is the contract between this editor and Ludus.
 * <p>
 * Four properties of the format matter enough to have their own tests. The {@code tilesets} array comes from
 * the tiles that the layers place, so a level lists the tilesets that it draws from. Repeated tilesets collapse
 * to one entry. The array is ordered by path, so one picture writes one file whatever order a user painted it
 * in. The {@code layers} array runs one entry per layer given, free layers included, so a position in it is the
 * layer index that a user stamped on.
 * <p>
 * Every tile here is built from a one pixel image, since {@link JsonUtil} reads the position of a tile and the
 * tileset that it was cut from rather than its pixels.
 *
 * @author Claude Code
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
class JsonUtilTest {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final String FIRST_PATH  = "/assets/terrain/tilesets/tilemap_color1.png";
    private static final String SECOND_PATH = "/assets/terrain/tilesets/tilemap_color2.png";
    private static final String THIRD_PATH  = "/assets/terrain/tilesets/tilemap_color3.png";

    private static final TileSet FIRST  = new TileSet(FIRST_PATH,  64);
    private static final TileSet SECOND = new TileSet(SECOND_PATH, 64);
    private static final TileSet THIRD  = new TileSet(THIRD_PATH,  64);

    private static final int LAYER_COUNT = 6;

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    @Test
    @DisplayName("A level states the size that it was written at")
    void testToJson_withValidArgs_ReturnsGivenRowAndColumnCount() {
        var layers = buildLayers(10, 11);

        var level = JsonUtil.toJson(10, 11, layers);

        assertAll(
                () -> assertEquals(10, level.get("rows").getAsInt()),
                () -> assertEquals(11, level.get("columns").getAsInt())
        );
    }

    @Test
    @DisplayName("A level with every cell free lists no tileset")
    void testToJson_withEmptyArgs_ReturnsEmptyTilesetArray() {
        var layers = buildLayers(10, 10);

        var level = JsonUtil.toJson(10, 10, layers);

        assertTrue(level.getAsJsonArray("tilesets").isEmpty());
    }

    @Test
    @DisplayName("A tileset reaches the file through the tile that was cut from it")
    void testToJson_withOnePlacedTile_ReturnsThatTilesetAlone() {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(6, 9, buildTile(SECOND, 5, 6));

        var tilesets = JsonUtil.toJson(10, 10, layers).getAsJsonArray("tilesets");

        assertAll(
                () -> assertEquals(1, tilesets.size()),
                () -> assertEquals(SECOND_PATH, tilesets.get(0).getAsJsonObject().get("path").getAsString()),
                () -> assertEquals(64, tilesets.get(0).getAsJsonObject().get("cellSize").getAsInt())
        );
    }

    @Test
    @DisplayName("Many tiles cut from one tileset write one tileset entry")
    void testToJson_withRepeatedTileset_ReturnsOneEntry() {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(0, 0, buildTile(FIRST, 0, 0));
        layers.get(0).placeTile(0, 1, buildTile(FIRST, 0, 1));
        layers.get(1).placeTile(4, 4, buildTile(FIRST, 3, 2));

        var tilesets = JsonUtil.toJson(10, 10, layers).getAsJsonArray("tilesets");

        assertEquals(1, tilesets.size());
    }

    @ParameterizedTest
    @CsvSource({
            "0, /assets/terrain/tilesets/tilemap_color1.png",
            "1, /assets/terrain/tilesets/tilemap_color2.png",
            "2, /assets/terrain/tilesets/tilemap_color3.png"
    })
    @DisplayName("The tileset array is ordered by path")
    void testToJson_withSeveralTilesets_ReturnsEntriesOrderedByPath(int position, String expected) {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(0, 0, buildTile(THIRD,  0, 0));
        layers.get(0).placeTile(0, 1, buildTile(FIRST,  0, 0));
        layers.get(0).placeTile(0, 2, buildTile(SECOND, 0, 0));

        var tilesets = JsonUtil.toJson(10, 10, layers).getAsJsonArray("tilesets");

        assertEquals(expected, tilesets.get(position).getAsJsonObject().get("path").getAsString());
    }

    @Test
    @DisplayName("Two levels drawing the same tilesets list them in the same order")
    void testToJson_withTilesetsAtSwappedPositions_ReturnsTheSameTilesetArray() {
        var painted = buildLayers(10, 10);
        painted.get(0).placeTile(0, 0, buildTile(THIRD, 1, 1));
        painted.get(0).placeTile(0, 1, buildTile(FIRST, 2, 2));

        var mirrored = buildLayers(10, 10);
        mirrored.get(0).placeTile(0, 0, buildTile(FIRST, 2, 2));
        mirrored.get(0).placeTile(0, 1, buildTile(THIRD, 1, 1));

        var first  = JsonUtil.toJson(10, 10, painted).getAsJsonArray("tilesets");
        var second = JsonUtil.toJson(10, 10, mirrored).getAsJsonArray("tilesets");

        assertEquals(first, second);
    }

    @Test
    @DisplayName("A tile's tileset property resolves against the file's own tileset array")
    void testToJson_withTilesFromTwoTilesets_ReturnsTheIndexOfEachTileset() {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(0, 0, buildTile(THIRD,  5, 6));
        layers.get(0).placeTile(0, 1, buildTile(FIRST,  1, 2));

        var level = JsonUtil.toJson(10, 10, layers);
        var tiles = level.getAsJsonArray("layers").get(0).getAsJsonObject().getAsJsonArray("tiles");

        assertAll(
                () -> assertEquals(THIRD_PATH, readTilesetPath(level, tiles.get(0).getAsJsonObject())),
                () -> assertEquals(FIRST_PATH, readTilesetPath(level, tiles.get(1).getAsJsonObject()))
        );
    }

    @Test
    @DisplayName("Every layer writes an entry, so a position in the array is a layer index")
    void testToJson_withFreeLayersBelowAPaintedOne_ReturnsAnEntryForEveryLayer() {
        var layers = buildLayers(10, 10);
        layers.get(3).placeTile(2, 2, buildTile(FIRST, 0, 0));

        var written = JsonUtil.toJson(10, 10, layers).getAsJsonArray("layers");

        assertAll(
                () -> assertEquals(LAYER_COUNT, written.size()),
                () -> assertTrue(readTiles(written, 0).isEmpty()),
                () -> assertTrue(readTiles(written, 1).isEmpty()),
                () -> assertTrue(readTiles(written, 2).isEmpty()),
                () -> assertEquals(1, readTiles(written, 3).size()),
                () -> assertTrue(readTiles(written, 4).isEmpty()),
                () -> assertTrue(readTiles(written, 5).isEmpty())
        );
    }

    @Test
    @DisplayName("Tiles are written by ascending row, then by ascending column")
    void testToJson_withTilesAcrossRowsAndColumns_ReturnsTilesOrderedRowFirst() {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(4, 7, buildTile(FIRST, 0, 0));
        layers.get(0).placeTile(2, 9, buildTile(FIRST, 0, 0));
        layers.get(0).placeTile(2, 3, buildTile(FIRST, 0, 0));

        var tiles = readTiles(JsonUtil.toJson(10, 10, layers).getAsJsonArray("layers"), 0);

        assertAll(
                () -> assertEquals(List.of(2, 2, 4), tiles.asList().stream().map(t -> t.getAsJsonObject().get("row").getAsInt()).toList()),
                () -> assertEquals(List.of(3, 9, 7), tiles.asList().stream().map(t -> t.getAsJsonObject().get("column").getAsInt()).toList())
        );
    }

    @Test
    @DisplayName("A tile states the cell of the tileset that it was cut from")
    void testToJson_withOnePlacedTile_ReturnsItsPositionAndItsSourceCell() {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(6, 9, buildTile(SECOND, 5, 6));

        var tile = readTiles(JsonUtil.toJson(10, 10, layers).getAsJsonArray("layers"), 0).get(0).getAsJsonObject();

        assertAll(
                () -> assertEquals(6, tile.get("row").getAsInt()),
                () -> assertEquals(9, tile.get("column").getAsInt()),
                () -> assertEquals(0, tile.get("tileset").getAsInt()),
                () -> assertEquals(5, tile.get("sourceRow").getAsInt()),
                () -> assertEquals(6, tile.get("sourceColumn").getAsInt())
        );
    }

    @Test
    @DisplayName("Two tilesets sharing a path stay apart where their cell sizes differ")
    void testToJson_withOnePathAtTwoCellSizes_ReturnsBothEntries() {
        var layers = buildLayers(10, 10);
        layers.get(0).placeTile(0, 0, buildTile(new TileSet(FIRST_PATH, 64), 0, 0));
        layers.get(0).placeTile(0, 1, buildTile(new TileSet(FIRST_PATH, 32), 0, 0));

        var tilesets = JsonUtil.toJson(10, 10, layers).getAsJsonArray("tilesets");

        assertEquals(2, tilesets.size());
    }

    @Test
    @DisplayName("A layer written against the wrong tileset list is refused")
    void testToJson_withTilesetOutsideTheGivenList_ThrowsIllegalArgumentException() {
        var layer = new TileGrid(10, 10);
        layer.placeTile(1, 1, buildTile(SECOND, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> JsonUtil.toJson(layer, List.of(FIRST)));
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Builds a stack of six free layers at the given size, which is what a {@code LevelCanvas} hands over.
     */
    private static List<TileGrid> buildLayers(int rows, int columns) {
        return IntStream.range(0, LAYER_COUNT)
                        .mapToObj(_ -> new TileGrid(rows, columns))
                        .toList();
    }

    private static TileSource buildTile(TileSet tileset, int sourceRow, int sourceColumn) {
        return new TileSource(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), tileset, sourceRow, sourceColumn);
    }

    private static JsonArray readTiles(JsonArray layers, int layer) {
        return layers.get(layer).getAsJsonObject().getAsJsonArray("tiles");
    }

    /**
     * Resolves a tile's {@code tileset} index against the level's own {@code tilesets} array, which is the step
     * that Ludus performs when it loads a level.
     */
    private static String readTilesetPath(JsonObject level, JsonObject tile) {
        var index = tile.get("tileset").getAsInt();

        return level.getAsJsonArray("tilesets").get(index).getAsJsonObject().get("path").getAsString();
    }

}
