package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.data.JsonUtil;
import com.slinky.ludus.editor.data.TileGrid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Converts a {@link LevelCanvas} to the JSON that Ludus loads.
 * <p>
 * {@link #buildLevelJson(LevelCanvas)} performs the whole conversion and returns the text, so a caller that
 * wants the JSON on its own calls that alone. {@link #saveLevel(LevelCanvas, Path)} calls it and writes the
 * result to a file. {@link JsonUtil#toJson(int, int, java.util.Collection)} states the format that both
 * produce.
 * <p>
 * <b>Converting a canvas that a user has painted</b>
 * <pre>{@code
 * var canvas = new LevelCanvas(64, 15, 15);
 * canvas.resizeGrid(10, 10);
 *
 * // after a user stamps one tile from tilemap_color2.png onto layer 0
 * var json = LevelWriter.buildLevelJson(canvas);
 *
 * // "rows": 10, "columns": 10, one entry in "tilesets", and six entries in "layers"
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *         <p>
 *         Last modified: 2026-09-10
 * @since 1.0.0
 */
final class LevelWriter {

    private LevelWriter() { }

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /**
     * Converts a canvas to the JSON that Ludus loads, reading every layer of it bottom first.
     *
     * @param levelCanvas the canvas to convert
     *
     * @return that level as pretty printed JSON
     */
    public static String buildLevelJson(LevelCanvas levelCanvas) {
        var layers = new ArrayList<TileGrid>(levelCanvas.getLayerCount());

        for (var layer = 0; layer < levelCanvas.getLayerCount(); layer++) {
            layers.add(levelCanvas.getLayer(layer));
        }

        var levelJson = JsonUtil.toJson(levelCanvas.getRows(), levelCanvas.getColumns(), layers);

        return JsonUtil.writePrettyString(levelJson);
    }

    /**
     * Converts a canvas and writes the JSON to the given file in UTF-8, creating the file where the directory
     * holding it already exists, and replacing the contents of a file that is already there.
     *
     * @param levelCanvas the canvas to convert
     * @param path        the file to write
     *
     * @return the JSON that {@link #buildLevelJson(LevelCanvas)} produced
     *
     * @throws UncheckedIOException if writing the file fails
     */
    public static String saveLevel(LevelCanvas levelCanvas, Path path) {
        var json = buildLevelJson(levelCanvas);

        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to write the level to '%s'", path), e);
        }

        return json;
    }

}
