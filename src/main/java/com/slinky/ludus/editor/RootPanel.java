package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.components.Stepper;
import com.slinky.ludus.editor.data.JsonUtil;
import com.slinky.ludus.editor.data.TileGrid;
import com.slinky.ludus.editor.panels.ControlBar;
import com.slinky.ludus.editor.panels.SwatchPanel;
import com.slinky.ludus.editor.panels.TitleBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.*;

/**
 * The window's content, with a {@link TitleBar} across the top, a {@link ControlBar} across the bottom, and the
 * space between them divided between a {@link SwatchPanel} down the left edge and a {@link LevelCanvas} filling
 * the rest. This panel owns the cell size and the grid maximum, and hands the same cell size to both, so a tile
 * occupies the same number of pixels wherever it appears.
 * <p>
 * A press on a swatch arms that tile on the canvas, and a press on the canvas stamps it. Three {@link Stepper}
 * controls in the control bar choose the layer that takes that stamp, and how many rows and columns the grid
 * runs to. Every wire runs through this panel, so the canvas keeps no reference to any control.
 * <p>
 * The editor opens on a grid of {@value #DEFAULT_ROWS} by {@value #DEFAULT_COLUMNS} cells, which a user steps
 * up towards {@value #MAX_ROWS} by {@value #MAX_COLUMNS}. A step that would drop a placed tile leaves the grid
 * and both captions where they are.
 * <p>
 * Each side occupies a {@link GridBagLayout} containing it as its only child, which lays it out at its preferred
 * size and centres it, so growing the window leaves both components centred in the space that they were given.
 * <p>
 * A margin of {@value #PADDING} pixels surrounds both sides, and the same distance separates one from the other,
 * so the two of them contribute {@code 3 * PADDING} pixels to the window's width and {@code 2 * PADDING} to its
 * height. The title bar and the control bar each span the full width outside that margin, and a single pixel
 * line runs round the whole panel, which is the edge that the window presents once a frame turns its own
 * decoration off.
 * <p>
 * <b>Opening the editor over a deck of tilesets</b>
 * <pre>{@code
 * var root = new RootPanel(tilesetPaths);
 *
 * frame.setUndecorated(true);
 * frame.setContentPane(root);
 * frame.pack();
 *
 * // the canvas takes 10 * 64 by 10 * 64 pixels, and the two bars add their own height above and below
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public class RootPanel extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    public static final int CELL_SIZE   = 64;
    public static final int MAX_ROWS    = 15;
    public static final int MAX_COLUMNS = 15;

    /** The grid that the editor opens on, which a user grows towards the maximum from the control bar. */
    public static final int DEFAULT_ROWS    = 10;
    public static final int DEFAULT_COLUMNS = 10;

    /** The distance in pixels between the window edge and either side, and between the two sides. */
    public static final int PADDING = 12;

    private static final Color WINDOW_EDGE = new Color(0, 0, 0, 40);

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final TitleBar    titleBar   = new TitleBar();
    private final ControlBar  controlBar = new ControlBar();
    private final SwatchPanel swatchPanel;
    private final LevelCanvas levelCanvas;

    private final Stepper layerStepper;
    private final Stepper rowStepper    = new Stepper("Rows",    1, MAX_ROWS,    DEFAULT_ROWS);
    private final Stepper columnStepper = new Stepper("Columns", 1, MAX_COLUMNS, DEFAULT_COLUMNS);

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a deck over the given tilesets and a canvas to stamp them onto, and wires one to the other.
     *
     * @param tilesetPaths the paths below {@value com.slinky.ludus.editor.components.Swatch#ROOT_DIR}, in the
     *                     order that the deck presents them
     * @throws IllegalArgumentException if no path is given
     */
    public RootPanel(String... tilesetPaths) {
        this.swatchPanel  = new SwatchPanel(CELL_SIZE, tilesetPaths);
        this.levelCanvas  = new LevelCanvas(CELL_SIZE, MAX_ROWS, MAX_COLUMNS);
        this.layerStepper = new Stepper("Layer", 0, levelCanvas.getLayerCount() - 1, 0);

        levelCanvas.resizeGrid(DEFAULT_ROWS, DEFAULT_COLUMNS);

        armCanvasOnSelection();
        selectCanvasLayerOnChange();
        resizeCanvasOnChange();

        controlBar.addLeading(layerStepper);
        controlBar.addLeading(rowStepper);
        controlBar.addLeading(columnStepper);

        var saveButton = new JButton("Save"); // TODO
        saveButton.addActionListener(_ -> saveLevel());

        controlBar.addTrailing(saveButton);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(WINDOW_EDGE));
        add(titleBar, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(controlBar, BorderLayout.SOUTH);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public TitleBar getTitleBar() {
        return titleBar;
    }

    public ControlBar getControlBar() {
        return controlBar;
    }

    public Stepper getLayerStepper() {
        return layerStepper;
    }

    public Stepper getRowStepper() {
        return rowStepper;
    }

    public Stepper getColumnStepper() {
        return columnStepper;
    }

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
     * Collects every layer of the canvas, bottom first, and prints the level as JSON.
     * {@link JsonUtil#toJson(int, int, java.util.Collection)} states the format that it prints.
     */
    private void saveLevel() {
        var layers = new ArrayList<TileGrid>();
        for (int i = 0; i < levelCanvas.getLayerCount(); i++) {
            layers.add(levelCanvas.getLayer(i));
        }

        var levelJson = JsonUtil.toJson(levelCanvas.getRows(), levelCanvas.getColumns(), layers);
        System.out.println(JsonUtil.writePrettyString(levelJson));
        // TODO: write to file
    }

    /**
     * Arms the canvas with the tile that a press selects, so the canvas learns the selection without knowing
     * which component produced it.
     */
    private void armCanvasOnSelection() {
        swatchPanel.addSelectionListener(_ -> swatchPanel.readSelectedTile().ifPresent(levelCanvas::setArmedTile));
    }

    /**
     * Points the canvas at the layer that the selector moves to, so the canvas learns which layer a press writes
     * to without knowing which control chose it.
     */
    private void selectCanvasLayerOnChange() {
        layerStepper.addValueListener(levelCanvas::setActiveLayer);
    }

    /**
     * Resizes the canvas whenever either dimension steps, and reads the two steppers together, since the canvas
     * takes both counts at once.
     */
    private void resizeCanvasOnChange() {
        rowStepper.addValueListener(rows -> applyGridSize(rows, columnStepper.getValue()));
        columnStepper.addValueListener(columns -> applyGridSize(rowStepper.getValue(), columns));
    }

    /**
     * Passes a size to the canvas, and returns both steppers to the size that stands where a tile would fall
     * outside the new bounds. The refusal comes back to the user as a step that leaves the caption where it was.
     */
    private void applyGridSize(int rows, int columns) {
        if (levelCanvas.canResizeTo(rows, columns)) {
            levelCanvas.resizeGrid(rows, columns);
            revalidate();
        } else {
            rowStepper.showValue(levelCanvas.getRows());
            columnStepper.showValue(levelCanvas.getColumns());
        }
    }

    /**
     * Lays the deck and the canvas out side by side inside the margin, which keeps the margin clear of the title
     * bar above.
     */
    private JPanel buildBody() {
        var body = new JPanel(new BorderLayout(PADDING, 0));
        body.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        body.add(buildCentredArea(swatchPanel), BorderLayout.WEST);
        body.add(buildCentredArea(levelCanvas), BorderLayout.CENTER);

        return body;
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
