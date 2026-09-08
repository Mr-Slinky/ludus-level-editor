package com.slinky.ludus.editor.data;

import com.google.gson.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 *
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 * @since 1.0.0
 */
public class JsonUtil {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Gson PRINTER = new GsonBuilder().setPrettyPrinting()
                                                         .serializeNulls()
                                                         .disableHtmlEscaping()
                                                         .create();

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    public static String writePrettyString(JsonElement json) {
        return PRINTER.toJson(json);
    }

    /**
     * Writes:
     * <pre>{@code
     *  {
     *   "rows": 10,
     *   "columns": 10,
     *   "tilesets": [
     *     {
     *       "path": "/assets/terrain/tilesets/tilemap_color1.png",
     *       "cellSize": 64
     *     }
     *   ],
     *   "layers": [
     *     {
     *       "tiles": [
     *         { "row": 8, "column": 3, "tileset": 0, "sourceRow": 1, "sourceColumn": 1 },
     *         { "row": 8, "column": 4, "tileset": 0, "sourceRow": 1, "sourceColumn": 1 }
     *       ]
     *     },
     *     {
     *       "tiles": [
     *         { "row": 8, "column": 4, "tileset": 0, "sourceRow": 5, "sourceColumn": 6 }
     *       ]
     *     },
     *     {
     *       "tiles": []
     *     }
     *   ]
     * }
     *  </pre>
     * <p>
     * The {@code tilesets} array is collected from the tiles that the layers place, so it lists exactly the
     * tilesets that the level draws from, ordered by path. Each tile's {@code tileset} property is its
     * tileset's position in that array.
     * <p>
     * The {@code layers} array runs one entry per layer given, in the order that they paint, bottom first. A
     * layer with every cell free writes an entry whose {@code tiles} array is empty, as the third entry above
     * does. Those entries are what keep a position in this array equal to the layer index that a user stamped
     * on, so a reader resolves layer 3 of a level as the fourth entry whatever the three below it contain.
     *
     * @param rows   the height of the level in cells
     * @param cols   the width of the level in cells
     * @param layers every layer of the canvas, bottom first
     * @return the level as a JSON object
     */
    public static JsonObject toJson(int rows, int cols, Collection<TileGrid> layers) {
        var tilesets = collectTileSets(layers);
        var jsonRoot = new JsonObject();

        jsonRoot.addProperty("rows", rows);
        jsonRoot.addProperty("columns", cols);

        var tilesetSet = new JsonArray();
        for (var tileset : tilesets) {
            var tilesetNode = toJson(tileset);
            tilesetSet.add(tilesetNode);
        }

        jsonRoot.add("tilesets", tilesetSet);

        var gridSet = new JsonArray();
        for (var layer : layers) {
            var layerNode = toJson(layer, tilesets);
            gridSet.add(layerNode);
        }

        jsonRoot.add("layers", gridSet);

        return jsonRoot;
    }

    /**
     * Writes:
     * <pre>{@code
     *  {
     *      "tiles": [
     *          { "row": 8, "column": 3, "tileset": 0, "sourceRow": 1, "sourceColumn": 1 },
     *          { "row": 8, "column": 4, "tileset": 0, "sourceRow": 1, "sourceColumn": 1 }
     *      ]
     *  }
     *  }
     *  </pre>
     *
     * @param tiles    the layer to write
     * @param tilesets the tilesets of the level, in the order that the file lists them
     * @return the layer as a JSON object
     * @throws IllegalArgumentException if the list omits a tileset that one of the layer's tiles was cut from
     */
    public static JsonObject toJson(TileGrid tiles, List<TileSet> tilesets) {
        var jsonRoot = new JsonObject();
        var tilesArr = new JsonArray();

        for (var tile : tiles.readPlacedTiles()) {
            var tileJson = toJson(tile, tilesets);
            tilesArr.add(tileJson);
        }

        jsonRoot.add("tiles", tilesArr);
        return jsonRoot;
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Collects the tilesets that the tiles of every layer were cut from, ordered by path, so one level writes
     * the same list whatever order a user stamped its tiles in.
     */
    private static List<TileSet> collectTileSets(Collection<TileGrid> layers) {
        // the comparator decides equality as well as order here, so it reads both components of a TileSet
        var found = new TreeSet<TileSet>(Comparator.comparing(TileSet::path).thenComparingInt(TileSet::cellSize));

        for (var layer : layers) {
            for (var tile : layer.readPlacedTiles()) {
                found.add(tile.tileset());
            }
        }

        return List.copyOf(found);
    }

    private static JsonObject toJson(TileSet tileset) {
        var tilesetJson = new JsonObject();

        tilesetJson.addProperty("path", tileset.path());
        tilesetJson.addProperty("cellSize", tileset.cellSize());

        return tilesetJson;
    }

    /**
     * Writes one tile, with its {@code tileset} property set to the position of its {@link TileSet} in the given
     * list, which is the index that a reader resolves against the file's own {@code tilesets} array.
     *
     * @throws IllegalArgumentException if the list omits the tileset that the tile was cut from
     */
    private static JsonObject toJson(TileGrid.PlacedTile tile, List<TileSet> tilesets) {
        var index = tilesets.indexOf(tile.tileset());

        if (index < 0) {
            throw new IllegalArgumentException(String.format("The given tilesets contain no entry for '%s', which the tile at row %d, column %d was cut from", tile.tileset().path(), tile.row(), tile.column()));
        }

        var tileJson = new JsonObject();

        tileJson.addProperty("row", tile.row());
        tileJson.addProperty("column", tile.column());
        tileJson.addProperty("tileset", index);
        tileJson.addProperty("sourceRow", tile.sourceRow());
        tileJson.addProperty("sourceColumn", tile.sourceColumn());

        return tileJson;
    }

}
