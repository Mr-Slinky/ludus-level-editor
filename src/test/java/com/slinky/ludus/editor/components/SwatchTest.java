package com.slinky.ludus.editor.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the press that picks a tile up and the second press that puts it back, which is how a user arms a canvas
 * and disarms it again.
 * <p>
 * Each test dispatches a real {@link MouseEvent} at the swatch and then reads {@link Swatch#getSelection()}, so
 * the conversion from a pixel position to a tile runs inside the build. The image is built in memory here,
 * since a swatch divides whatever pixels it is given and reads nothing else.
 * <p>
 * Tiles are {@value #TILE_SIZE} pixels square across an image of three columns by two rows.
 *
 * @author Claude Code
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-10
 * @since 1.0.0
 */
class SwatchTest {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final int TILE_SIZE = 64;
    private static final int COLUMNS   = 3;
    private static final int ROWS      = 2;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private Swatch swatch;

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    @BeforeEach
    void buildSwatch() {
        var art = new BufferedImage(COLUMNS * TILE_SIZE, ROWS * TILE_SIZE, BufferedImage.TYPE_INT_ARGB);

        swatch = new Swatch(art, TILE_SIZE);
    }

    @Test
    @DisplayName("A press selects the tile under the pointer")
    void testMousePressed_withPointInsideATile_SelectsThatTile() {
        pressAt(2 * TILE_SIZE + 10, TILE_SIZE + 10);

        assertAll(
                () -> assertTrue(swatch.hasSelection()),
                () -> assertEquals(new Point(2, 1), swatch.getSelection().orElseThrow())
        );
    }

    @Test
    @DisplayName("A second press on the selected tile clears the selection")
    void testMousePressed_withTwoPressesOnOneTile_ClearsTheSelection() {
        pressAt(TILE_SIZE + 10, 10);
        pressAt(TILE_SIZE + 20, 20);

        assertAll(
                () -> assertFalse(swatch.hasSelection()),
                () -> assertTrue(swatch.getSelection().isEmpty())
        );
    }

    @Test
    @DisplayName("A press on another tile moves the selection to it")
    void testMousePressed_withPressesOnTwoTiles_SelectsTheSecond() {
        pressAt(10, 10);
        pressAt(2 * TILE_SIZE + 10, TILE_SIZE + 10);

        assertEquals(new Point(2, 1), swatch.getSelection().orElseThrow());
    }

    @Test
    @DisplayName("A third press on a cleared tile selects it again")
    void testMousePressed_withThreePressesOnOneTile_SelectsItAgain() {
        pressAt(10, 10);
        pressAt(10, 10);
        pressAt(10, 10);

        assertEquals(new Point(0, 0), swatch.getSelection().orElseThrow());
    }

    @Test
    @DisplayName("Every press reports to a listener, whether it selects a tile or clears one")
    void testAddSelectionListener_withAPressThatSelectsAndOneThatClears_ReportsBoth() {
        List<Boolean> reported = new ArrayList<>();

        swatch.addSelectionListener(() -> reported.add(swatch.hasSelection()));

        pressAt(10, 10);
        pressAt(10, 10);

        assertAll(
                () -> assertEquals(2, reported.size()),
                () -> assertTrue(reported.get(0)),
                () -> assertFalse(reported.get(1))
        );
    }

    @Test
    @DisplayName("A clear from a container leaves every listener unnotified")
    void testClearSelection_withASelectedTile_ReportsNothingToListeners() {
        List<Boolean> reported = new ArrayList<>();

        pressAt(10, 10);
        swatch.addSelectionListener(() -> reported.add(swatch.hasSelection()));
        swatch.clearSelection();

        assertAll(
                () -> assertTrue(reported.isEmpty()),
                () -> assertFalse(swatch.hasSelection())
        );
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Dispatches a press at a point in component pixels, which is the gesture that selects a tile and, on a
     * tile already selected, clears it.
     */
    private void pressAt(int x, int y) {
        swatch.dispatchEvent(new MouseEvent(
                swatch, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false
        ));
    }

}
