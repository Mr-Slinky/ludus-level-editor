package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.panels.SwatchPanel;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * The window's content, dividing it between a {@link SwatchPanel} down the left edge and a {@link LevelCanvas}
 * filling the rest. This panel owns the cell size and the grid maximum, and hands the same cell size to both, so
 * a tile occupies the same number of pixels wherever it appears.
 * <p>
 * A press on a swatch arms that tile on the canvas, and a press on the canvas stamps it. The canvas holds no
 * reference to the swatch panel, since this panel listens to one and arms the other.
 * <p>
 * Each side occupies a {@link GridBagLayout} containing it as its only child, which lays it out at its preferred
 * size and centres it, so growing the window leaves both components centred in the space they were given.
 * <p>
 * A margin of {@value #PADDING} pixels surrounds both sides, and the same distance separates one from the other,
 * so the two of them contribute {@code 3 * PADDING} pixels to the window's width and {@code 2 * PADDING} to its
 * height.
 * <p>
 * <b>Opening the editor over a deck of tilesets</b>
 * <pre>{@code
 * var root = new RootPanel("terrain/tilesets/Tilemap_color1.png");
 *
 * frame.setContentPane(root);
 * frame.pack();
 *
 * // the canvas takes 15 * 64 by 15 * 64 pixels, so the content measures 984 pixels tall
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class RootPanel extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    public static final int CELL_SIZE   = 64;
    public static final int MAX_COLUMNS = 15;
    public static final int MAX_ROWS    = 15;

    /** The distance in pixels between the window edge and either side, and between the two sides. */
    public static final int PADDING = 12;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final SwatchPanel swatchPanel;
    private final LevelCanvas levelCanvas;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a deck over the given tilesets and a canvas to stamp them onto, and wires one to the other.
     *
     * @param tilesetPaths the paths below {@value com.slinky.ludus.editor.components.Swatch#ROOT_DIR}, in the
     *                     order the deck presents them
     * @throws IllegalArgumentException if no path is given
     */
    public RootPanel(String... tilesetPaths) {
        this.swatchPanel = new SwatchPanel(CELL_SIZE, tilesetPaths);
        this.levelCanvas = new LevelCanvas(CELL_SIZE, MAX_COLUMNS, MAX_ROWS);

        armCanvasOnSelection();

        setLayout(new BorderLayout(PADDING, 0));
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        add(buildCentredArea(swatchPanel), BorderLayout.WEST);
        add(buildCentredArea(levelCanvas), BorderLayout.CENTER);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public SwatchPanel getSwatchPanel() {
        return swatchPanel;
    }

    public LevelCanvas getLevelCanvas() {
        return levelCanvas;
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Arms the canvas with the tile a press selects, so the canvas learns the selection without knowing which
     * component produced it.
     */
    private void armCanvasOnSelection() {
        swatchPanel.addSelectionListener(selection -> swatchPanel.readSelectedImage().ifPresent(levelCanvas::setArmedTile));
    }

    /**
     * Wraps a component in a {@link GridBagLayout}, which lays a single child out at its preferred size and
     * centres it in whatever room the layout gives it.
     */
    private JPanel buildCentredArea(JPanel content) {
        var area = new JPanel(new GridBagLayout());
        area.add(content);

        return area;
    }

}
