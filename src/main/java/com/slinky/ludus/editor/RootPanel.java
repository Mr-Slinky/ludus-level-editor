package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.ActionButton;
import com.slinky.ludus.editor.components.CanvasStage;
import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.components.MetadataSwatch;
import com.slinky.ludus.editor.components.Stepper;
import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.panels.ControlBar;
import com.slinky.ludus.editor.panels.MenuBar;
import com.slinky.ludus.editor.panels.MetadataView;
import com.slinky.ludus.editor.panels.SwatchPanel;
import com.slinky.ludus.editor.panels.TitleBar;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.nio.file.Path;

import javax.swing.*;

/**
 * The window's content, with a {@link TitleBar} across the top, a {@link ControlBar} across the bottom, and the
 * space between them divided between the swatches down the left edge and a {@link LevelCanvas} filling the
 * rest. This panel owns the cell size and the grid maximum, and hands the same cell size to every swatch and to
 * the canvas, so a tile occupies the same number of pixels wherever it appears.
 * <p>
 * A {@link MenuBar} at the left end of the title bar switches the left edge between two swatches: the
 * {@link SwatchPanel} of tilesets, and a {@link MetadataSwatch} of metadata blocks. A press on either swatch
 * arms its selection on the canvas, and a press on the canvas stamps it. Switching swatches re-arms the canvas
 * with the selection on the swatch now shown, and disarms it where that swatch has nothing selected, so a press
 * always stamps from the swatch on screen.
 * <p>
 * A {@link MetadataView} under the swatches shows the metadata of the cell selected on the canvas, and updates
 * whenever the canvas reports a change to its selection.
 * <p>
 * Three {@link Stepper} controls in the control bar choose the layer that takes a stamp, and how many rows and
 * columns the grid runs to. Every wire runs through this panel, so the canvas keeps no reference to any control.
 * <p>
 * The editor opens on a grid of {@value #DEFAULT_ROWS} by {@value #DEFAULT_COLUMNS} cells, which a user steps
 * up towards {@value #MAX_ROWS} by {@value #MAX_COLUMNS}. A step that would drop a placed tile leaves the grid
 * and both captions where they are.
 * <p>
 * Each side stands on a surface that fills the height of the window. On the left, the metadata view takes its
 * own height at the bottom of the surface and the visible swatch fills the space above it, so a tileset shorter
 * than the window leaves the surface itself running down to the metadata view. On the right, the canvas stands
 * on a {@link CanvasStage}, which centres the grid while the window has room for it and scrolls to the grid
 * once it outgrows that room.
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
 *          Last modified: 2026-09-10
 * @since 1.0.0
 */
public class RootPanel extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The width and height in pixels of one cell, on the canvas and on every swatch alike. */
    public static final int CELL_SIZE   = 64;

    /** The tallest grid that the canvas accepts, in cells. */
    public static final int MAX_ROWS    = 15;

    /** The widest grid that the canvas accepts, in cells. */
    public static final int MAX_COLUMNS = 15;

    /** The number of rows that the editor opens on, which a user grows towards {@link #MAX_ROWS}. */
    public static final int DEFAULT_ROWS    = 10;

    /** The number of columns that the editor opens on, which a user grows towards {@link #MAX_COLUMNS}. */
    public static final int DEFAULT_COLUMNS = 10;

    /** The distance in pixels between the window edge and either side, and between the two sides. */
    public static final int PADDING = 12;

    private static final Color GROUND      = Palette.getActive().getDark();
    private static final Color WINDOW_EDGE = Palette.withAlpha(Palette.getActive().getLight(), 40);

    /** The accent that a palette reserves for the action writing a file, which the save button fills with. */
    private static final Color SAVE_FILL = Palette.getActive().getAccent3();

    /**
     * The surface that the swatches and the canvas stand on. It lifts less far from the ground than the two bars
     * do, so the window reads at three depths: the ground in the margins, the surfaces on it, and the bars
     * above both.
     */
    private static final Color SURFACE      = Palette.blend(Palette.getActive().getDark(), Palette.getActive().getLight(), 0.035f);
    private static final Color SURFACE_EDGE = Palette.withAlpha(Palette.getActive().getLight(), 22);

    /**
     * The distance in pixels between the edge of a surface and its content, and between the swatches and the
     * metadata view below them.
     */
    private static final int SURFACE_PADDING = 10;

    /** The names of the two swatch cards, which the menu also draws as its item labels. */
    private static final String TILES_CARD    = "Tiles";
    private static final String METADATA_CARD = "Metadata";

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final TitleBar       titleBar       = new TitleBar();
    private final ControlBar     controlBar     = new ControlBar();
    private final MenuBar        menuBar        = new MenuBar();
    private final MetadataSwatch metadataSwatch = new MetadataSwatch(CELL_SIZE);
    private final MetadataView   metadataView   = new MetadataView();
    private final SwatchPanel    swatchPanel;
    private final LevelCanvas    levelCanvas;

    private final CardLayout swatchDeck  = new CardLayout();
    private final JPanel     swatchCards = new JPanel(swatchDeck);

    private final Stepper layerStepper;
    private final Stepper rowStepper    = new Stepper("Rows",    1, MAX_ROWS,    DEFAULT_ROWS);
    private final Stepper columnStepper = new Stepper("Columns", 1, MAX_COLUMNS, DEFAULT_COLUMNS);

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a deck over the given tilesets, a metadata swatch, a metadata view and a canvas to stamp onto, and
     * wires them together.
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
        armCanvasOnMetadataSelection();
        showMetadataOnSelection();
        selectCanvasLayerOnChange();
        resizeCanvasOnChange();
        populateMenu();

        titleBar.addLeading(menuBar);

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

    public MenuBar getMenuBar() {
        return menuBar;
    }

    public MetadataSwatch getMetadataSwatch() {
        return metadataSwatch;
    }

    public MetadataView getMetadataView() {
        return metadataView;
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\

    /**
     * Hands the canvas to {@link LevelWriter#saveLevel(LevelCanvas, Path)}, which converts it and writes the
     * result to {@code newLevel.json} in the directory that the editor runs from. The save button in the
     * control bar calls this.
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
     * Arms the canvas with the metadata block that a press selects, in the same way that
     * {@link #armCanvasOnSelection()} arms it with a tile.
     */
    private void armCanvasOnMetadataSelection() {
        metadataSwatch.addSelectionListener(_ -> metadataSwatch.readSelectedMetadata().ifPresent(levelCanvas::setArmedMetadata));
    }

    /**
     * Refreshes the metadata view each time the canvas reports a change to its selection.
     */
    private void showMetadataOnSelection() {
        levelCanvas.addSelectionListener(this::showSelectedMetadata);
    }

    /**
     * Shows the selected cell in the metadata view: its metadata where the cell stores a tile on the active
     * layer, a caption alone where the cell is free, and the opening state where no cell is selected.
     */
    private void showSelectedMetadata() {
        var cell = levelCanvas.getSelectedCell();

        if (cell.isEmpty()) {
            metadataView.showNoSelection();
            return;
        }

        levelCanvas.readSelectedMetadata().ifPresentOrElse(
                data -> metadataView.showMetadata(cell.get(), data),
                () -> metadataView.showEmptyCell(cell.get())
        );
    }

    /**
     * Adds one menu item per swatch card, in the order that the menu shows them.
     */
    private void populateMenu() {
        menuBar.addItem(TILES_CARD,    this::showTileSwatch);
        menuBar.addItem(METADATA_CARD, this::showMetadataSwatch);
    }

    /**
     * Shows the tile deck and re-arms the tile selected on it. With no tile selected, the canvas disarms, so a
     * press never stamps metadata from a swatch that is out of sight.
     */
    private void showTileSwatch() {
        swatchDeck.show(swatchCards, TILES_CARD);
        swatchPanel.readSelectedTile().ifPresentOrElse(levelCanvas::setArmedTile, levelCanvas::clearArmedMetadata);
    }

    /**
     * Shows the metadata swatch and re-arms the block selected on it. With no block selected, the canvas disarms,
     * so a press never stamps a tile from a swatch that is out of sight.
     */
    private void showMetadataSwatch() {
        swatchDeck.show(swatchCards, METADATA_CARD);
        metadataSwatch.readSelectedMetadata().ifPresentOrElse(levelCanvas::setArmedMetadata, levelCanvas::clearArmedTile);
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
     * Lays the swatches and the canvas out side by side inside the margin, which keeps the margin clear of the
     * title bar above.
     */
    private JPanel buildBody() {
        var body = new JPanel(new BorderLayout(PADDING, 0));
        body.setBackground(GROUND);
        body.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        body.add(buildDock(buildSwatchArea()), BorderLayout.WEST);
        body.add(buildStage(levelCanvas), BorderLayout.CENTER);

        return body;
    }

    /**
     * Stacks the swatch cards above the metadata view, with {@value #SURFACE_PADDING} pixels between them. The
     * visible card fills whatever height the metadata view leaves.
     */
    private JPanel buildSwatchArea() {
        swatchCards.setOpaque(false);
        swatchCards.add(swatchPanel, TILES_CARD);
        swatchCards.add(buildMetadataCard(), METADATA_CARD);

        var area = new JPanel(new BorderLayout(0, SURFACE_PADDING));
        area.setOpaque(false);
        area.add(swatchCards, BorderLayout.CENTER);
        area.add(metadataView, BorderLayout.SOUTH);

        return area;
    }

    /**
     * Lays the metadata swatch out at its own size against the top left of the card, so a press on the empty
     * space around it selects no block.
     */
    private JPanel buildMetadataCard() {
        var card = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.setBackground(GROUND);
        card.add(metadataSwatch);

        return card;
    }

    /**
     * Builds the surface down the left edge, which runs the full height of the body and surrounds the given
     * content with a margin of {@value #SURFACE_PADDING} pixels.
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
     * Builds the surface that the canvas stands on, which fills the rest of the body. The stage takes
     * {@value #CELL_SIZE} as its scroll increment, so scrolling moves the grid one cell at a time.
     */
    private CanvasStage buildStage(JPanel content) {
        return new CanvasStage(content, CELL_SIZE);
    }

}
