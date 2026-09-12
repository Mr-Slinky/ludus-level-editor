package com.slinky.ludus.editor;

import com.slinky.ludus.editor.components.LevelCanvas;
import com.slinky.ludus.editor.components.MetadataSwatch;
import com.slinky.ludus.editor.components.Stepper;
import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.panels.ControlBar;
import com.slinky.ludus.editor.panels.MenuBar;
import com.slinky.ludus.editor.panels.MetadataView;
import com.slinky.ludus.editor.panels.SwatchPanel;
import com.slinky.ludus.editor.panels.TitleBar;
import com.slinky.ludus.ui.SmallButton;
import com.slinky.ludus.ui.Surface;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

import javax.swing.*;

/**
 * The window's content, with a {@link TitleBar} across the top, a {@link ControlBar} across the bottom, and the
 * space between them divided between the swatches on the left and a {@link LevelCanvas} on the right. This
 * panel owns the cell size and the grid maximum, and hands the same cell size to every swatch and to the
 * canvas, so a tile occupies the same number of pixels wherever it appears.
 * <p>
 * A {@link MenuBar} at the left end of the title bar switches the left edge between two swatches: the
 * {@link SwatchPanel} of tilesets, and a {@link MetadataSwatch} of metadata blocks. One selection stands across
 * the two of them, so picking a tile clears whatever the metadata swatch had picked, and picking a metadata
 * block clears the tile. The canvas is armed with that one selection, and a second press on the piece already
 * picked clears it and disarms the canvas.
 * <p>
 * A press on the canvas selects the cell under the pointer and alters no layer. The E key stamps the armed
 * piece into the cell under the pointer, which is the cell that the canvas outlines while anything is armed.
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
 * The swatches stand on a wooden {@link Surface}, and the canvas stands on a special paper one. Each surface
 * draws at a set of sizes one middle piece apart, so this panel chooses a size for each of the two, and the
 * window then takes its own size from them.
 * <p>
 * This panel grows the table one piece at a time until the space inside its frame fits the swatch area, with
 * {@value #SURFACE_PADDING} pixels between the two. The paper draws one middle piece per grid cell, so a step
 * of either grid stepper adds or removes one piece, and this panel then packs the window around the size that
 * results.
 * <p>
 * A margin of {@value #PADDING} pixels surrounds both surfaces, and the same distance separates one from the
 * other, so the two of them contribute {@code 3 * PADDING} pixels to the window's width and {@code 2 * PADDING}
 * to its height. The two surfaces line up at the top, each at the size it asks for, and the shorter one leaves
 * the window's ground on show beneath it. The title bar and the control bar each span the full width outside
 * that margin, and a single pixel line runs round the whole panel, which is the edge that the window presents
 * once a frame turns its own decoration off.
 * <p>
 * <b>Opening the editor over a deck of tilesets</b>
 * <p>
 * A caller builds the panel over the tileset paths, puts it in an undecorated frame, and packs that frame:
 * <pre>{@code
 * var root = new RootPanel(tilesetPaths);
 *
 * frame.setUndecorated(true);
 * frame.setContentPane(root);
 * frame.pack();
 *
 * // the grid opens at 10 by 10 cells, and the paper under it draws one middle piece per cell
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-12
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

    /**
     * The thickness in pixels of the frame that the wooden surface draws round its table top, measured from the
     * art as the top, the left, the bottom and the right. The bottom covers the front edge of the table as well
     * as the frame, which is why it runs deepest.
     */
    private static final Insets WOOD_FRAME = new Insets(23, 20, 38, 20);

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
    private final JPanel     paperSlot   = new JPanel(new BorderLayout());

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
        placeCanvasOnPaper();

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
     * Clears the metadata swatch each time a press changes what the tile swatch has selected, then arms the
     * canvas with the result, so one selection stands across the two swatches.
     */
    private void armCanvasOnSelection() {
        swatchPanel.addSelectionListener(() -> {
            metadataSwatch.clearSelection();
            applyArmedStamp();
        });
    }

    /**
     * Clears the tile swatch each time a press changes what the metadata swatch has selected, in the same way
     * that {@link #armCanvasOnSelection()} clears the metadata swatch.
     */
    private void armCanvasOnMetadataSelection() {
        metadataSwatch.addSelectionListener(() -> {
            swatchPanel.clearSelection();
            applyArmedStamp();
        });
    }

    /**
     * Arms the canvas with whichever swatch has a selection, and disarms it where neither of them has one. One
     * swatch is cleared before this runs, so the canvas is armed with the one piece that a user has picked out,
     * and a press of E stamps that piece alone.
     */
    private void applyArmedStamp() {
        var tile = swatchPanel.readSelectedTile();

        if (tile.isPresent()) {
            levelCanvas.setArmedTile(tile.get());
            return;
        }

        var data = metadataSwatch.readSelectedMetadata();

        if (data.isPresent()) {
            levelCanvas.setArmedMetadata(data.get());
            return;
        }

        levelCanvas.clearArmedTile();
        levelCanvas.clearArmedMetadata();
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
     * Shows the tile swatch. One selection stands across both swatches, so the card on screen decides what a
     * user can pick next and leaves what is armed as it stands.
     */
    private void showTileSwatch() {
        swatchDeck.show(swatchCards, TILES_CARD);
    }

    /**
     * Shows the metadata swatch, in the same way that {@link #showTileSwatch()} shows the tile swatch.
     */
    private void showMetadataSwatch() {
        swatchDeck.show(swatchCards, METADATA_CARD);
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
            placeCanvasOnPaper();
            fitWindow();
        } else {
            rowStepper.showValue(levelCanvas.getRows());
            columnStepper.showValue(levelCanvas.getColumns());
        }
    }

    /**
     * Replaces the paper under the canvas with a {@link Surface} of one middle piece per grid cell, so the sheet
     * grows and shrinks with the grid. Adding the canvas to the new paper removes it from the previous one,
     * since a Swing component has one parent.
     */
    private void placeCanvasOnPaper() {
        var paper = Surface.specialPaper(levelCanvas.getColumns(), levelCanvas.getRows());
        paper.setLayout(new GridBagLayout());
        paper.add(levelCanvas);

        paperSlot.removeAll();
        paperSlot.add(paper, BorderLayout.CENTER);
    }

    /**
     * Packs the window around this panel, which sizes it to the room that the two surfaces and the two bars now
     * ask for. A panel that belongs to no window lays itself out again instead.
     */
    private void fitWindow() {
        var window = SwingUtilities.getWindowAncestor(this);

        if (window == null) {
            revalidate();
        } else {
            window.pack();
        }
    }

    /**
     * Builds the save button as a blue square {@link SmallButton} under the shield icon, and wires a press to
     * {@link #saveLevel()}.
     */
    private SmallButton buildSaveButton() {
        var saveButton = SmallButton.blueSquare(SmallButton.Symbol.SHIELD, 0);
        saveButton.setToolTipText("Save Level");

        saveButton.addActionListener(_ -> saveLevel());

        return saveButton;
    }

    /**
     * Lays the table and the paper side by side inside the margin, each at the size it asks for and aligned at
     * the top. The paper stands in a slot of its own, so {@link #placeCanvasOnPaper()} swaps one paper for
     * another and leaves this layout as it is.
     */
    private JPanel buildBody() {
        var body = new JPanel(new GridBagLayout());
        body.setBackground(GROUND);
        body.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        var place = new GridBagConstraints();
        place.anchor = GridBagConstraints.NORTH;
        body.add(buildTable(buildSwatchArea()), place);

        place.insets = new Insets(0, PADDING, 0, 0);
        paperSlot.setOpaque(false);
        body.add(paperSlot, place);

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
     * Builds the wooden {@link Surface} under the swatch area, at the smallest size whose inside fits that
     * area.
     * <p>
     * The border keeps the content clear of the frame in the art by {@link #WOOD_FRAME}, plus
     * {@value #SURFACE_PADDING} pixels on every side. Each call to {@code increaseWidth} adds one middle piece
     * to the width and each call to {@code increaseHeight} adds one to the height, so the two loops step the
     * table up to the first size that fits.
     */
    private Surface buildTable(JPanel content) {
        var table = Surface.wood(0, 0);
        table.setLayout(new GridBagLayout());
        table.setBorder(BorderFactory.createEmptyBorder(
                WOOD_FRAME.top    + SURFACE_PADDING,
                WOOD_FRAME.left   + SURFACE_PADDING,
                WOOD_FRAME.bottom + SURFACE_PADDING,
                WOOD_FRAME.right  + SURFACE_PADDING));
        table.add(content);

        var needed = content.getPreferredSize();
        var frame  = table.getInsets();

        while (table.getPreferredSize().width - frame.left - frame.right < needed.width) {
            table.increaseWidth();
        }

        while (table.getPreferredSize().height - frame.top - frame.bottom < needed.height) {
            table.increaseHeight();
        }

        return table;
    }

}
