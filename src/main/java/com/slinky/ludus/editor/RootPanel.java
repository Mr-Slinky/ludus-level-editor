package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.ActionButton;
import com.slinky.ludus.editor.components.CanvasStage;
import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.components.Stepper;
import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.panels.ControlBar;
import com.slinky.ludus.editor.panels.SwatchPanel;
import com.slinky.ludus.editor.panels.TitleBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.nio.file.Path;

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
 * Each side stands on a surface that fills the height of the window. The deck fills the surface holding it, so
 * a tileset shorter than the window leaves the swatch's own ground running down to the bottom edge. The canvas
 * sits in a {@link GridBagLayout} instead, which lays a single child out at its preferred size and centres it,
 * so growing the window leaves the grid in the middle of the room that it was given.
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

    private static final Color GROUND      = Palette.getActive().getDark();
    private static final Color WINDOW_EDGE = Palette.withAlpha(Palette.getActive().getLight(), 40);

    /** The accent that a palette reserves for the action writing a file, which the save button fills with. */
    private static final Color SAVE_FILL = Palette.getActive().getAccent3();

    /**
     * The surface that the deck and the canvas stand on. It lifts less far from the ground than the two bars
     * do, so the window reads at three depths: the ground in the margins, the surfaces on it, and the bars
     * above both.
     */
    private static final Color SURFACE      = Palette.blend(Palette.getActive().getDark(), Palette.getActive().getLight(), 0.035f);
    private static final Color SURFACE_EDGE = Palette.withAlpha(Palette.getActive().getLight(), 22);

    /** The distance in pixels between the deck and the edges of the surface holding it. */
    private static final int SURFACE_PADDING = 10;

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

        controlBar.addTrailing(buildSaveButton());

        setLayout(new BorderLayout());
        setBackground(GROUND);
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
     * Hands the canvas to {@link LevelWriter#saveLevel(LevelCanvas, Path)}, which converts it and writes the
     * result. The save button in the control bar calls this.
     */
    private void saveLevel() {
        LevelWriter.saveLevel(levelCanvas, Path.of("newLevel.json"));
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
     * Builds the save button in the accent that a palette reserves for the save action, which makes it the one
     * control in the bar that a user picks out by colour.
     */
    private ActionButton buildSaveButton() {
        var saveButton = new ActionButton("Save", SAVE_FILL);

        saveButton.addActionListener(_ -> saveLevel());

        return saveButton;
    }

    /**
     * Lays the deck and the canvas out side by side inside the margin, which keeps the margin clear of the title
     * bar above.
     */
    private JPanel buildBody() {
        var body = new JPanel(new BorderLayout(PADDING, 0));
        body.setBackground(GROUND);
        body.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        body.add(buildDock(swatchPanel), BorderLayout.WEST);
        body.add(buildStage(levelCanvas), BorderLayout.CENTER);

        return body;
    }

    /**
     * Builds the surface down the left edge, with the deck against its top edge and the surface running the
     * full height of the body below it. A deck shorter than the window leaves room at the bottom of a panel
     * that a user can see the edges of, rather than a gap in the window behind it.
     */
    private JPanel buildDock(JPanel content) {
        var dock = new JPanel(new BorderLayout());
        dock.setBackground(SURFACE);
        dock.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE_EDGE),
                BorderFactory.createEmptyBorder(SURFACE_PADDING, SURFACE_PADDING, SURFACE_PADDING, SURFACE_PADDING)));
        dock.add(content, BorderLayout.CENTER);

        return dock;
    }

    /**
     * Builds the surface that the canvas stands on, which fills the rest of the body. A {@link CanvasStage}
     * centres the grid while the window has room for it, and scrolls to it a cell at a time once a step past
     * {@value #DEFAULT_ROWS} rows or columns grows the grid beyond that room.
     */
    private CanvasStage buildStage(JPanel content) {
        return new CanvasStage(content, CELL_SIZE);
    }

}
