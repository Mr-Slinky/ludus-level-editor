package com.slinky.ludus.editor.panels;

import com.slinky.ludus.editor.components.ChevronButton;
import com.slinky.ludus.editor.components.Swatch;
import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.data.TileSource;
import com.slinky.ludus.editor.data.TileSet;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Stacks several {@link Swatch} instances into a deck and shows one at a time, with a back and a forward button
 * either side of a caption stating the name of the visible tileset. Calling {@link #showNext()} on the last
 * swatch shows the first, and calling {@link #showPrevious()} on the first shows the last.
 * <p>
 * One tile stays selected across the whole deck. A press on the visible swatch clears the selection on every
 * other swatch, so {@link #getSelection()} answers for the whole deck, and a selection made on one tileset
 * survives flipping away and back.
 * <p>
 * {@link #readSelectedTile()} returns that selection as a {@link TileSource}, together with the {@link TileSet}
 * that the swatch it was selected on loaded its image from. A tile therefore identifies its own tileset by path,
 * and a caller writing a level out reads that path from the tile rather than from the position of any swatch.
 * <p>
 * <b>Building a deck from the terrain tilesets</b>
 * <p>
 * A caller builds a deck, flips to the second tileset, and reads back the tile that a press selected:
 * <pre>{@code
 * var deck = new SwatchPanel(64, "terrain/tilesets/tilemap_color1.png", "terrain/tilesets/tilemap_color2.png");
 *
 * deck.showNext();
 *
 * // the caption now states the second tileset's file name, followed by "(2 of 2)"
 * deck.getVisibleIndex();                          // 1
 *
 * // after a press on the third tile in the second row of that swatch
 * deck.getSelection();                             // Optional[java.awt.Point[x=2,y=1]]
 * deck.readSelectedTile().get().tileset().path();  // "/assets/terrain/tilesets/tilemap_color2.png"
 * deck.readSelectedTile().get().sourceRow();       // 1
 * deck.readSelectedTile().get().sourceColumn();    // 2
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public class SwatchPanel extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color GROUND = Palette.getActive().getDark();

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final List<Swatch.SelectionListener> listeners = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private final List<String> names    = new ArrayList<>();

    private final CardLayout deck    = new CardLayout();
    private final JPanel     cards   = new JPanel(deck);
    private final JLabel     caption = new JLabel("", SwingConstants.CENTER);

    private int index;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Loads one swatch per image path and shows the first of them.
     *
     * @param tileSize   the tile width and height in pixels, applied to every swatch in the deck
     * @param imagePaths the paths below {@value Swatch#ROOT_DIR}, in the order that the deck presents them
     * @throws IllegalArgumentException if no path is given
     */
    public SwatchPanel(int tileSize, String... imagePaths) {
        if (imagePaths.length == 0) {
            throw new IllegalArgumentException("A swatch panel requires at least one image path");
        }

        for (var path : imagePaths) {
            addSwatch(new Swatch(path, tileSize), deriveName(path));
        }

        cards.setBackground(GROUND);
        caption.setForeground(Palette.getActive().getLight());

        setLayout(new BorderLayout());
        setBackground(GROUND);
        add(buildNavigationBar(), BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);

        showSwatch(0);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public int getSwatchCount() {
        return swatches.size();
    }

    public int getVisibleIndex() {
        return index;
    }

    public Swatch getVisibleSwatch() {
        return swatches.get(index);
    }

    /**
     * Returns the tile selected anywhere in the deck, in the tile coordinates of the swatch that it belongs to.
     *
     * @return the selected tile, and empty while no swatch in the deck has one
     */
    public Optional<Point> getSelection() {
        return findSelectedSwatch().flatMap(Swatch::getSelection);
    }

    /**
     * Reports whether any swatch in the deck has a selected tile.
     *
     * @return {@code true} where {@link #getSelection()} returns a tile
     */
    public boolean hasSelection() {
        return findSelectedSwatch().isPresent();
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Shows the swatch at the given position and updates the caption.
     *
     * @param position the zero based position in the deck
     * @throws IndexOutOfBoundsException if the position falls outside the deck
     */
    public void showSwatch(int position) {
        if (position < 0 || position >= swatches.size()) {
            throw new IndexOutOfBoundsException(String.format("A deck of %d swatches has no position %d", swatches.size(), position));
        }

        index = position;
        deck.show(cards, names.get(index));
        caption.setText(String.format("%s (%d of %d)", names.get(index), index + 1, swatches.size()));
    }

    /**
     * Shows the next swatch, wrapping to the first one after the last.
     */
    public void showNext() {
        showSwatch((index + 1) % swatches.size());
    }

    /**
     * Shows the previous swatch, wrapping to the last one before the first.
     */
    public void showPrevious() {
        showSwatch((index + swatches.size() - 1) % swatches.size());
    }

    /**
     * Returns the region of the source image the selection covers, at that image's own resolution.
     *
     * @return the selected pixels, and empty while no swatch in the deck has a selection
     */
    public Optional<BufferedImage> readSelectedImage() {
        return findSelectedSwatch().flatMap(Swatch::readSelectedImage);
    }

    /**
     * Returns the selected tile, with {@link TileSource#tileset()} set to the tileset that the swatch it was
     * selected on loaded its image from.
     *
     * @return the selected tile, and empty where no swatch in the deck has a selection, or where the selected
     *         swatch was built over an image already in memory, which leaves it without a path to write
     */
    public Optional<TileSource> readSelectedTile() {
        var found = findSelectedSwatch();

        if (found.isEmpty()) {
            return Optional.empty();
        }

        var swatch  = found.get();
        var tileset = readTileSet(swatch);

        if (tileset.isEmpty()) {
            return Optional.empty();
        }

        return swatch.getSelection()
                     .flatMap(tile -> swatch.readSelectedImage()
                                            .map(image -> new TileSource(image, tileset.get(), tile.y, tile.x)));
    }

    /**
     * Empties the selection on every swatch in the deck, leaving listeners unnotified.
     */
    public void clearSelection() {
        swatches.forEach(Swatch::clearSelection);
    }

    /**
     * Registers a listener that receives the selected tile every time a press changes it.
     *
     * @param listener the listener to notify
     */
    public void addSelectionListener(Swatch.SelectionListener listener) {
        listeners.add(listener);
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    private void addSwatch(Swatch swatch, String name) {
        swatch.addSelectionListener(selection -> handleSwatchSelection(swatch, selection));

        swatches.add(swatch);
        names.add(name);
        cards.add(swatch, name);
    }

    /**
     * Clears every swatch other than the one just pressed, so one tile stays selected across the deck, then
     * passes the selection on. Each clear stays silent, so none of them re-enters this method.
     */
    private void handleSwatchSelection(Swatch source, Point selection) {
        for (var swatch : swatches) {
            if (swatch != source) {
                swatch.clearSelection();
            }
        }

        for (var listener : listeners) {
            listener.handleSelection(selection);
        }
    }

    private JPanel buildNavigationBar() {
        var previous = new ChevronButton(ChevronButton.Direction.LEFT);
        var next     = new ChevronButton(ChevronButton.Direction.RIGHT);

        previous.addActionListener(_ -> showPrevious());
        next.addActionListener(_ -> showNext());

        var bar = new JPanel(new BorderLayout());
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bar.add(previous, BorderLayout.WEST);
        bar.add(caption, BorderLayout.CENTER);
        bar.add(next, BorderLayout.EAST);

        return bar;
    }

    /**
     * Describes the tileset that one swatch cuts its tiles from, as the classpath resource that it loaded its
     * image from and the pixel width of one tile.
     *
     * @param swatch the swatch to describe
     * @return that tileset, and empty for a swatch built over an image already in memory, which leaves it
     *         without a path to write
     */
    private Optional<TileSet> readTileSet(Swatch swatch) {
        return swatch.getResourcePath().map(path -> new TileSet(path, swatch.getTileWidth()));
    }

    private OptionalInt findSelectedIndex() {
        return IntStream.range(0, swatches.size())
                        .filter(index -> swatches.get(index).hasSelection())
                        .findFirst();
    }

    private Optional<Swatch> findSelectedSwatch() {
        var found = findSelectedIndex();

        return found.isPresent() ? Optional.of(swatches.get(found.getAsInt())) : Optional.empty();
    }

    /**
     * Reads the file name out of a resource path and drops the extension, so a path ending {@code /name.png}
     * captions as {@code name}.
     */
    private String deriveName(String path) {
        var start = path.lastIndexOf('/') + 1;
        var end   = path.lastIndexOf('.');

        return end > start ? path.substring(start, end) : path.substring(start);
    }

}
