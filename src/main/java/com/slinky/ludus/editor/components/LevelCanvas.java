package com.slinky.ludus.editor.components;

import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.data.TileData;
import com.slinky.ludus.editor.data.TileGrid;
import com.slinky.ludus.editor.data.TileSource;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * A stack of {@value #MAX_LAYERS} grids, one per layer, that a caller stamps tiles onto. Each cell is the size
 * of one tile in a {@link Swatch}, and every layer shares one row and column count.
 * <p>
 * The panel takes the pixel size of the grid as both its preferred and its minimum size. A layout with room to
 * spare therefore centres the canvas, and a layout with less room clips it, so the grid stays on screen at
 * whatever size the window happens to be.
 * <p>
 * The layers paint in index order, so layer 0 goes down first and each layer after it draws over the one below.
 * A tile's transparent pixels let the layers beneath show through, which puts the overhanging face of a cliff
 * over the ground behind it. Every layer stays visible while a user edits, so a level appears exactly as it
 * will appear once it is finished.
 * <p>
 * A tile whose metadata sets {@link TileData#hasShadow()} draws the image read from {@value #SHADOW_ASSET}
 * centred on its cell, and that image overhangs the cell on every side. Each layer draws its shadows after all
 * of its tiles. Therefore, a shadow falls across the neighbouring tiles of its own layer, and the tiles of every
 * higher layer draw over it.
 * <p>
 * One layer at a time takes a stamp. {@link #setActiveLayer(int)} chooses it, {@link #getActiveLayer()} reports
 * it, and {@link #getGrid()} returns its tiles. A press writes to that layer alone and leaves the rest as they
 * stand.
 * <p>
 * Every cell starts as water, drawn beneath layer 0 in the colour read from {@value #WATER_ASSET}, and a stamped
 * tile covers that colour. A level therefore begins as an expanse of water that a user builds land into.
 * {@link #getWaterColour()} returns that colour, which a caller writing the level out uses for every cell still
 * showing it.
 * <p>
 * A press inside the grid selects the cell under the pointer and leaves every layer as it stands, so a user
 * reads a cell without altering it. The canvas marks the selected cell with a tinted fill and an outline.
 * {@link #getSelectedCell()} returns that cell, and {@link #readSelectedMetadata()} returns the metadata
 * stored in it on the active layer. A caller follows both through
 * {@link #addSelectionListener(SelectionListener)}.
 * <p>
 * Stamping goes through {@link #stampHoveredCell()}, which the E key runs while the window holding this canvas
 * has the keyboard focus. A caller arms a {@link TileSource} through {@link #setArmedTile(TileSource)}, and
 * each stamp then writes that tile into the cell under the pointer. The armed tile stays armed, so one tile
 * can be stamped as many times as a user likes. While a tile is armed, the cell under the pointer shows it at
 * reduced opacity inside an outline, so a user sees where the next stamp will land before making it.
 * <p>
 * A caller arms {@link TileData} through {@link #setArmedMetadata(TileData)} in the same way, and a stamp then
 * sets each flag of that metadata on the tile under the pointer, wherever the active layer stores a tile there.
 * The tile keeps every flag that an earlier stamp set, so a stamp that marks a shadow leaves a traversable tile
 * traversable. While metadata is armed, the cell under the pointer shows the outline alone. One stamp is armed
 * at a time, so arming a tile disarms the metadata, and arming metadata disarms the tile.
 * <p>
 * A press of the right button frees the cell under the pointer on the active layer, removing its tile and its
 * metadata together, and selects that cell as any other press does. Whatever is armed stays armed through it,
 * so the next stamp lands as before.
 * <p>
 * {@link TileGrid} stores the placed tiles of one layer. A resize goes through {@link #resizeGrid(int, int)},
 * which sets every layer at once. Growing keeps every tile where it is, and shrinking keeps the tiles that stay
 * inside the new bounds. A shrink that would drop a tile is refused, so every tile stays inside the grid, and
 * {@link #canResizeTo(int, int)} reports in advance whether a given size is accepted.
 * <p>
 * <b>Stamping a cliff face over the ground behind it</b>
 * <p>
 * A caller arms one tile, presses twice, and reads back where the art of the first press came from:
 * <pre>{@code
 * void stampCliff(BufferedImage art) {
 *     var canvas  = new LevelCanvas(64, 15, 15);
 *     var tileset = new TileSet("/assets/terrain/tilesets/tilemap_color1.png", 64);
 *
 *     // 15 rows of 64 pixels by 15 columns of 64 pixels
 *     canvas.getPreferredSize();     // java.awt.Dimension[width=960,height=960]
 *
 *     canvas.setArmedTile(new TileSource(art, tileset, 1, 1));
 *
 *     // with the pointer at (300, 140), a press of E stamps row 2, column 4 of layer 0
 *     var stamped = canvas.getGrid().readTile(2, 4);
 *
 *     stamped.get().tileset().path();  // "/assets/terrain/tilesets/tilemap_color1.png"
 *     stamped.get().sourceColumn();    // 1
 *
 *     canvas.setActiveLayer(1);
 *
 *     // a stamp on the same cell now writes to layer 1, and layer 0 keeps its tile
 *     canvas.getLayer(0).readTile(2, 4).isPresent();  // true
 * }
 * }</pre>
 * <p>
 * <b>Marking a stamped tile as traversable</b>
 * <p>
 * A caller stamps a tile, arms metadata, stamps the same cell again, and reads the metadata back through the
 * selection:
 * <pre>{@code
 * void markWalkable(TileSource grass) {
 *     var canvas = new LevelCanvas(64, 15, 15);
 *
 *     canvas.setArmedTile(grass);
 *     // with the pointer at (300, 140), a press of E writes the tile into row 2, column 4 of layer 0
 *
 *     canvas.setArmedMetadata(new TileData(true, false));
 *     // a second press of E writes the metadata into that same cell
 *
 *     // a press of the pointer there selects the cell
 *     canvas.getSelectedCell();                             // Optional[java.awt.Point[x=4,y=2]]
 *     canvas.readSelectedMetadata().get().isTraversable();  // true
 *     canvas.getArmedTile();                                // Optional.empty
 * }
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-14
 * @since 1.0.0
 */
public class LevelCanvas extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The asset that the water colour is read from, below {@value Swatch#ROOT_DIR}. */
    public static final String WATER_ASSET = "terrain/tilesets/water-background-color.png";

    /** The asset that the shadow image is read from, below {@value Swatch#ROOT_DIR}. */
    public static final String SHADOW_ASSET = "terrain/tilesets/shadow.png";

    /** The number of grids a canvas stacks, indexed 0 at the bottom to {@code MAX_LAYERS - 1} at the top. */
    public static final int MAX_LAYERS = 6;

    private static final Color BACKGROUND        = Palette.getActive().getDark();
    private static final Color GRID_COLOUR       = Palette.getActive().getGridLine();
    private static final Color HOVER_OUTLINE     = Palette.getActive().getAccent1();
    private static final Color SELECTION_FILL    = Palette.withAlpha(Palette.getActive().getAccent1(), 60);
    private static final Color SELECTION_OUTLINE = Palette.getActive().getAccent1();

    private static final float HOVER_ALPHA = 0.45f;

    /**
     * The cell size that the shadow image is drawn against. The image is three of these cells square, so its
     * central cell covers the shadowed cell and the rest overhangs the cells around it.
     */
    private static final int SHADOW_CELL_SIZE = 64;

    /** The width in pixels of the outline round the selected cell, drawn inside the cell. */
    private static final int SELECTION_STROKE = 2;

    /** The name that the E key and the action it runs share in this canvas's input and action maps. */
    private static final String STAMP_ACTION = "stampHoveredCell";

    /**
     * Returns the colour filling {@value #WATER_ASSET}, taken from its top left pixel. The asset is a single
     * flat colour, so any pixel in it answers the same.
     */
    private static Color readWaterColour() {
        var image = Swatch.loadImage(WATER_ASSET);

        return new Color(image.getRGB(0, 0), true);
    }

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<TileGrid> layers = new ArrayList<>();
    private final List<SelectionListener> listeners = new ArrayList<>();
    private final Color waterColour = readWaterColour();
    private final BufferedImage shadow = Swatch.loadImage(SHADOW_ASSET);
    private final int cellWidth;
    private final int cellHeight;
    private final int maxRows;
    private final int maxColumns;

    private int        activeLayer;
    private TileSource armedTile;
    private TileData   armedMetadata;
    private Point      hovered;
    private Point      selected;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a canvas of square cells, with the grid starting at its largest.
     *
     * @param cellSize   the cell width and height in pixels
     * @param maxRows    the tallest that the grid can be, in cells
     * @param maxColumns the widest that the grid can be, in cells
     */
    public LevelCanvas(int cellSize, int maxRows, int maxColumns) {
        this(cellSize, cellSize, maxRows, maxColumns);
    }

    /**
     * Builds a canvas of cells of the given size, with the grid starting at its largest.
     *
     * @param cellWidth  the cell width in pixels
     * @param cellHeight the cell height in pixels
     * @param maxRows    the tallest that the grid can be, in cells
     * @param maxColumns the widest that the grid can be, in cells
     * @throws IllegalArgumentException if any argument is zero or negative
     */
    public LevelCanvas(int cellWidth, int cellHeight, int maxRows, int maxColumns) {
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException(String.format("Cell size must be positive, given %d by %d", cellWidth, cellHeight));
        }

        if (maxRows <= 0 || maxColumns <= 0) {
            throw new IllegalArgumentException(String.format("The grid maximum must be positive, given %d rows by %d columns", maxRows, maxColumns));
        }

        this.cellWidth  = cellWidth;
        this.cellHeight = cellHeight;
        this.maxRows    = maxRows;
        this.maxColumns = maxColumns;

        for (var layer = 0; layer < MAX_LAYERS; layer++) {
            layers.add(new TileGrid(maxRows, maxColumns));
        }

        applyGridSize();
        setBackground(BACKGROUND);
        installMouseHandling();
        installKeyHandling();
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /**
     * Returns the tiles of the layer that a press currently stamps onto.
     *
     * @return the active layer's grid
     */
    public TileGrid getGrid() {
        return readActiveGrid();
    }

    /**
     * Returns the tiles of one layer, active or otherwise.
     *
     * @param layer the index, from 0 at the bottom to {@code MAX_LAYERS - 1} at the top
     * @return that layer's grid
     * @throws IndexOutOfBoundsException if the index addresses no layer
     */
    public TileGrid getLayer(int layer) {
        requireLayer(layer);

        return layers.get(layer);
    }

    /**
     * Returns the index of the layer that a press currently stamps onto.
     *
     * @return the active index, from 0 at the bottom to {@code MAX_LAYERS - 1} at the top
     */
    public int getActiveLayer() {
        return activeLayer;
    }

    public int getLayerCount() {
        return layers.size();
    }

    public int getRows() {
        return readActiveGrid().getRows();
    }

    public int getColumns() {
        return readActiveGrid().getColumns();
    }

    /**
     * Returns the colour that every cell shows before a tile lands on it.
     *
     * @return the colour read from {@value #WATER_ASSET}
     */
    public Color getWaterColour() {
        return waterColour;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getMaxColumns() {
        return maxColumns;
    }

    /**
     * Returns the tile that a press will stamp.
     *
     * @return the armed {@link TileSource}, and empty while no tile is armed
     */
    public Optional<TileSource> getArmedTile() {
        return Optional.ofNullable(armedTile);
    }

    /**
     * Returns the metadata that a press will stamp.
     *
     * @return the armed {@link TileData}, and empty while no metadata is armed
     */
    public Optional<TileData> getArmedMetadata() {
        return Optional.ofNullable(armedMetadata);
    }

    /**
     * Returns the selected cell in cell coordinates, where {@code x} is the column and {@code y} is the row.
     *
     * @return a copy of the selected cell, and empty while no cell is selected
     */
    public Optional<Point> getSelectedCell() {
        return Optional.ofNullable(selected).map(Point::new);
    }

    // ========================================================================================== \\
    //                                          Setters                                           \\
    // ========================================================================================== \\
    /**
     * Arms a tile and disarms any metadata. Every later stamp writes the tile into the cell under the pointer,
     * until a caller arms something else.
     *
     * @param tile the tile to stamp
     * @throws IllegalArgumentException if the tile is null
     */
    public void setArmedTile(TileSource tile) {
        if (tile == null) {
            throw new IllegalArgumentException("An armed tile requires a tile source");
        }

        armedTile     = tile;
        armedMetadata = null;
        repaint();
    }

    /**
     * Arms metadata and disarms any tile. Every later stamp sets each flag of the metadata on the tile under the
     * pointer, wherever the active layer stores a tile in that cell, until a caller arms something else.
     *
     * @param data the metadata to stamp
     * @throws IllegalArgumentException if the data is null
     */
    public void setArmedMetadata(TileData data) {
        if (data == null) {
            throw new IllegalArgumentException("An armed stamp requires tile data");
        }

        armedMetadata = data;
        armedTile     = null;
        repaint();
    }

    /**
     * Chooses the layer that every later press stamps onto. The other layers keep their tiles and stay visible, so
     * the change alters where a stamp lands and leaves the picture alone.
     *
     * @param layer the index, from 0 at the bottom to {@code MAX_LAYERS - 1} at the top
     * @throws IndexOutOfBoundsException if the index addresses no layer
     */
    public void setActiveLayer(int layer) {
        requireLayer(layer);

        activeLayer = layer;
        fireSelectionChanged();
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Stamps whatever is armed into the cell under the pointer, which is the cell that the ghost outlines. The
     * E key runs this while the window holding this canvas has the keyboard focus.
     * <p>
     * A stamp writes the armed tile into that cell, or sets each flag of the armed metadata on the tile already
     * there.
     * Every layer stays as it stands where nothing is armed, where the pointer rests away from the grid, and
     * where armed metadata meets a free cell.
     */
    public void stampHoveredCell() {
        if (hovered != null) {
            stampCell(hovered);
        }
    }

    /**
     * Disarms the tile and leaves any armed metadata armed.
     */
    public void clearArmedTile() {
        armedTile = null;
        repaint();
    }

    /**
     * Disarms the metadata and leaves any armed tile armed.
     */
    public void clearArmedMetadata() {
        armedMetadata = null;
        repaint();
    }

    /**
     * Returns the metadata stored in the selected cell on the active layer.
     *
     * @return that cell's {@link TileData}, and empty while no cell is selected or the selected cell is free on
     *         the active layer
     */
    public Optional<TileData> readSelectedMetadata() {
        if (selected == null) {
            return Optional.empty();
        }

        var grid = readActiveGrid();

        return grid.readTile(selected.y, selected.x).isPresent()
                ? Optional.of(grid.readMetadata(selected.y, selected.x))
                : Optional.empty();
    }

    /**
     * Registers a listener that the canvas calls after every event that can change what
     * {@link #getSelectedCell()} or {@link #readSelectedMetadata()} returns. Those events are a press inside the
     * grid, a stamp, a change of active layer, a clear, and a resize that deselects the selected cell.
     *
     * @param listener the listener to notify
     */
    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Empties every cell of the active layer and repaints, leaving the other layers as they stand.
     */
    public void clearGrid() {
        readActiveGrid().clear();
        repaint();
        fireSelectionChanged();
    }

    /**
     * Empties every cell of every layer and repaints, which also leaves every size within the maximum available
     * to {@link #resizeGrid(int, int)}.
     */
    public void clearAllLayers() {
        layers.forEach(TileGrid::clear);
        repaint();
        fireSelectionChanged();
    }

    /**
     * Resizes every layer and lays the canvas out again at the new pixel size. All layers share one size, so a
     * resize either sets all of them or sets none. Every tile inside the new bounds keeps its cell, and the canvas
     * deselects a selected cell that falls outside them.
     *
     * @param rows    the new height in cells
     * @param columns the new width in cells
     * @throws IllegalStateException    if a tile stands outside the new bounds on any layer
     * @throws IllegalArgumentException if the size falls outside one cell to the maximum
     */
    public void resizeGrid(int rows, int columns) {
        requireSize(rows, columns);

        if (!canResizeTo(rows, columns)) {
            throw new IllegalStateException(String.format("A size of %d rows by %d columns would drop %d tiles", rows, columns, countTilesOutside(rows, columns)));
        }

        layers.forEach(layer -> layer.resize(rows, columns));

        if (selected != null && !readActiveGrid().contains(selected.y, selected.x)) {
            selected = null;
            fireSelectionChanged();
        }

        applyGridSize();
        revalidate();
        repaint();
    }

    /**
     * Reports whether a resize would keep every tile currently placed.
     *
     * @param rows    the height to test, in cells
     * @param columns the width to test, in cells
     * @return {@code true} while every tile on every layer stands inside those bounds
     * @throws IllegalArgumentException if the size falls outside one cell to the maximum
     */
    public boolean canResizeTo(int rows, int columns) {
        requireSize(rows, columns);

        return countTilesOutside(rows, columns) == 0;
    }

    /**
     * Counts the cells holding a tile across every layer.
     *
     * @return the number of tiles placed, from zero to the cell count times the layer count
     */
    public int countPlacedTiles() {
        return layers.stream()
                     .mapToInt(TileGrid::getPlacedCount)
                     .sum();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        var canvas = (Graphics2D) g.create();
        paintWater(canvas);
        paintTiles(canvas);
        paintGrid(canvas);
        paintSelection(canvas);
        paintHover(canvas);
        canvas.dispose();
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private void installMouseHandling() {
        var mouse = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    eraseAt(e.getPoint());
                }

                selectAt(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                trackHover(findCell(e.getPoint()).orElse(null));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                trackHover(findCell(e.getPoint()).orElse(null));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                trackHover(null);
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /**
     * Binds the E key to {@link #stampHoveredCell()}. The binding stands on the whole window rather than on
     * this component, so a user stamps without giving the canvas the keyboard focus first.
     */
    private void installKeyHandling() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), STAMP_ACTION);
        getActionMap().put(STAMP_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                stampHoveredCell();
            }
        });
    }

    /**
     * Writes the armed tile or the armed metadata into one cell of the active layer, then reports the change to
     * every selection listener, since a stamp on the selected cell alters what
     * {@link #readSelectedMetadata()} returns. With nothing armed, every layer stays as it stands.
     */
    private void stampCell(Point cell) {
        if (armedTile != null) {
            readActiveGrid().placeTile(cell.y, cell.x, armedTile);
        } else if (armedMetadata != null) {
            stampMetadataAt(cell);
        } else {
            return;
        }

        repaint();
        fireSelectionChanged();
    }

    /**
     * Combines the armed metadata with the metadata of a cell of the active layer that stores a tile, and leaves
     * a free cell as it stands.
     */
    private void stampMetadataAt(Point cell) {
        var grid = readActiveGrid();

        // a written level includes the metadata of occupied cells alone
        if (grid.readTile(cell.y, cell.x).isPresent()) {
            var combined = grid.readMetadata(cell.y, cell.x).combine(armedMetadata);
            grid.placeMetadata(cell.y, cell.x, combined);
        }
    }

    /**
     * Frees the cell of the active layer containing the point, removing its tile and its metadata and leaving
     * every other layer as it stands. A point outside the grid frees nothing.
     */
    private void eraseAt(Point point) {
        findCell(point).ifPresent(cell -> {
            readActiveGrid().removeTile(cell.y, cell.x);
            repaint();
        });
    }

    /**
     * Selects the cell containing the point and tells every listener, even where that cell was already selected,
     * since the press that selected it may also have changed what it contains.
     */
    private void selectAt(Point point) {
        findCell(point).ifPresent(cell -> {
            selected = cell;
            repaint();
            fireSelectionChanged();
        });
    }

    private void fireSelectionChanged() {
        for (var listener : listeners) {
            listener.handleSelectionChange();
        }
    }

    /**
     * Converts a point in component pixels to the cell containing it.
     *
     * @return the cell, and empty for a point outside the grid
     */
    private Optional<Point> findCell(Point point) {
        if (point.x < 0 || point.y < 0) {
            return Optional.empty();
        }

        var row    = point.y / cellHeight;
        var column = point.x / cellWidth;

        return readActiveGrid().contains(row, column) ? Optional.of(new Point(column, row)) : Optional.empty();
    }

    /**
     * Remembers the cell under the pointer and repaints once it changes, which keeps the ghost to one repaint per
     * cell crossed rather than one per pixel moved.
     */
    private void trackHover(Point cell) {
        if (Objects.equals(cell, hovered)) {
            return;
        }

        hovered = cell;
        repaint();
    }

    /**
     * Fills the grid with the water colour, which every later stage paints over.
     */
    private void paintWater(Graphics2D canvas) {
        canvas.setColor(waterColour);
        canvas.fillRect(0, 0, getColumns() * cellWidth, getRows() * cellHeight);
    }

    /**
     * Draws every layer in index order, so layer 0 goes down first and each layer after it covers the one below
     * wherever its tiles are opaque.
     */
    private void paintTiles(Graphics2D canvas) {
        for (var layer : layers) {
            paintLayer(canvas, layer);
        }
    }

    /**
     * Draws every tile of one layer, then the shadow of each tile on that layer whose metadata sets
     * {@link TileData#hasShadow()}. A shadow overhangs its cell, so the tiles go down in full first, which keeps a
     * tile drawn later in the pass from covering the overhang of a shadow beside it.
     */
    private void paintLayer(Graphics2D canvas, TileGrid layer) {
        for (var row = 0; row < layer.getRows(); row++) {
            for (var column = 0; column < layer.getColumns(); column++) {
                var x = column * cellWidth;
                var y = row * cellHeight;

                layer.readTile(row, column)
                     .ifPresent(tile -> canvas.drawImage(tile.image(), x, y, cellWidth, cellHeight, null));
            }
        }

        for (var tile : layer.readPlacedTiles()) {
            if (tile.data().hasShadow()) {
                paintShadow(canvas, tile.row(), tile.column());
            }
        }
    }

    /**
     * Draws the shadow image centred on one cell. The image scales by the cell size over
     * {@value #SHADOW_CELL_SIZE}, which lays its central cell over this one and the rest over the cells around it.
     */
    private void paintShadow(Graphics2D canvas, int row, int column) {
        var width  = shadow.getWidth()  * cellWidth  / SHADOW_CELL_SIZE;
        var height = shadow.getHeight() * cellHeight / SHADOW_CELL_SIZE;
        var left   = column * cellWidth  - (width  - cellWidth)  / 2;
        var top    = row    * cellHeight - (height - cellHeight) / 2;

        canvas.drawImage(shadow, left, top, width, height, null);
    }

    private void paintGrid(Graphics2D canvas) {
        var right  = getColumns() * cellWidth  - 1;
        var bottom = getRows()    * cellHeight - 1;

        canvas.setColor(GRID_COLOUR);

        for (var row = 0; row <= getRows(); row++) {
            var y = Math.min(row * cellHeight, bottom);
            canvas.drawLine(0, y, right, y);
        }

        for (var column = 0; column <= getColumns(); column++) {
            var x = Math.min(column * cellWidth, right);
            canvas.drawLine(x, 0, x, bottom);
        }
    }

    /**
     * Marks the selected cell with a tinted fill and an outline drawn inside the cell.
     */
    private void paintSelection(Graphics2D canvas) {
        if (selected == null) {
            return;
        }

        var x = selected.x * cellWidth;
        var y = selected.y * cellHeight;

        canvas.setColor(SELECTION_FILL);
        canvas.fillRect(x, y, cellWidth, cellHeight);

        // the outline draws inside the cell, so the stroke is inset by half its width at every edge
        var inset   = SELECTION_STROKE / 2;
        var outline = (Graphics2D) canvas.create();

        outline.setColor(SELECTION_OUTLINE);
        outline.setStroke(new BasicStroke(SELECTION_STROKE));
        outline.drawRect(x + inset, y + inset, cellWidth - SELECTION_STROKE, cellHeight - SELECTION_STROKE);
        outline.dispose();
    }

    /**
     * Outlines the cell under the pointer while anything is armed, and draws an armed tile inside the outline at
     * reduced opacity, so a user sees where the next stamp will land before making it.
     */
    private void paintHover(Graphics2D canvas) {
        if (hovered == null || (armedTile == null && armedMetadata == null)) {
            return;
        }

        var x = hovered.x * cellWidth;
        var y = hovered.y * cellHeight;

        if (armedTile != null) {
            var ghost = (Graphics2D) canvas.create();
            ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, HOVER_ALPHA));
            ghost.drawImage(armedTile.image(), x, y, cellWidth, cellHeight, null);
            ghost.dispose();
        }

        canvas.setColor(HOVER_OUTLINE);
        canvas.drawRect(x, y, cellWidth - 1, cellHeight - 1);
    }

    /**
     * Sets the panel's preferred and minimum size to the pixel size of the grid. Both matter: a layout with room
     * to spare reads the preferred size and centres the canvas, and a layout with less room reads the minimum
     * size, which keeps the grid drawn and clipped rather than shrunk away to nothing.
     */
    private void applyGridSize() {
        var size = new Dimension(getColumns() * cellWidth, getRows() * cellHeight);

        setPreferredSize(size);
        setMinimumSize(size);
    }

    /**
     * Counts the tiles standing outside the given bounds, across every layer.
     */
    private int countTilesOutside(int rows, int columns) {
        var outside = 0;

        for (var layer : layers) {
            for (var row = 0; row < layer.getRows(); row++) {
                for (var column = 0; column < layer.getColumns(); column++) {
                    if ((row >= rows || column >= columns) && layer.readTile(row, column).isPresent()) {
                        outside++;
                    }
                }
            }
        }

        return outside;
    }

    private void requireSize(int rows, int columns) {
        if (rows < 1 || rows > maxRows || columns < 1 || columns > maxColumns) {
            throw new IllegalArgumentException(String.format("A size of %d rows by %d columns falls outside 1 by 1 to %d rows by %d columns", rows, columns, maxRows, maxColumns));
        }
    }

    /**
     * Returns the grid that a press stamps onto, which every method reaching for the current layer goes through.
     */
    private TileGrid readActiveGrid() {
        return layers.get(activeLayer);
    }

    private void requireLayer(int layer) {
        if (layer < 0 || layer >= layers.size()) {
            throw new IndexOutOfBoundsException(String.format("A stack of %d layers has no layer %d", layers.size(), layer));
        }
    }

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
    /**
     * Receives a call whenever the selected cell on a {@link LevelCanvas}, or the metadata stored in it, can have
     * changed. The listener reads the new state back from the canvas.
     */
    @FunctionalInterface
    public interface SelectionListener {

        /**
         * Handles a change to the selected cell, or to the metadata stored in it.
         */
        void handleSelectionChange();
    }

}
