package com.slinky.ludus.editor.panels;

import com.slinky.ludus.editor.data.Palette;
import com.slinky.ludus.editor.data.TileData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A panel that shows the metadata of one cell, as a caption identifying the cell above one row per
 * {@link TileData} component. Each row pairs the component's name, in a muted tone, with its value in the accent
 * that a palette gives to a chosen value.
 * <p>
 * Three methods set what the panel shows. {@link #showMetadata(Point, TileData)} shows the metadata of a cell
 * that stores a tile, {@link #showEmptyCell(Point)} captions a free cell, and {@link #showNoSelection()} returns
 * the panel to the state that it opens in. Every method takes a cell as a {@link Point} whose {@code x} is the
 * column and whose {@code y} is the row, both counting from 0.
 * <p>
 * <b>Showing the metadata of one cell</b>
 * <p>
 * A caller shows the metadata of a traversable tile, then shows the same cell once its tile is removed:
 * <pre>{@code
 * var view = new MetadataView();
 *
 * view.showMetadata(new Point(4, 2), new TileData(true, false));
 * // the caption shows "Row 2, column 4", the Traversable row shows "Yes", and the Shadow row shows "No"
 *
 * view.showEmptyCell(new Point(4, 2));
 * // the caption shows "Row 2, column 4, no tile", and both rows show "-"
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *         <p>
 *         Last modified: 2026-09-14
 * @since 1.0.0
 */
public class MetadataView extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color GROUND         = Palette.getActive().getDark();
    private static final Color SEPARATOR      = Palette.getActive().getSeparator();
    private static final Color CAPTION_COLOUR = Palette.getActive().getLight();
    private static final Color NAME_COLOUR    = Palette.withAlpha(Palette.getActive().getLight(), 170);
    private static final Color VALUE_COLOUR   = Palette.getActive().getAccent2();

    /** The value that every row shows after {@link #showEmptyCell(Point)} or {@link #showNoSelection()}. */
    private static final String BLANK = "-";

    private static final int PADDING    = 8;
    private static final int ROW_GAP    = 4;
    private static final int NAME_SIZE  = 11;
    private static final int VALUE_SIZE = 13;

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final JLabel caption     = new JLabel("", SwingConstants.LEFT);
    private final JLabel traversable = new JLabel(BLANK, SwingConstants.RIGHT);
    private final JLabel shadow      = new JLabel(BLANK, SwingConstants.RIGHT);

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a view in the state that {@link #showNoSelection()} sets.
     */
    public MetadataView() {
        caption.setForeground(CAPTION_COLOUR);

        var rows = new JPanel(new GridLayout(0, 2, 0, ROW_GAP));
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(ROW_GAP, 0, 0, 0));
        addRow(rows, "Traversable", traversable);
        addRow(rows, "Shadow", shadow);

        setLayout(new BorderLayout());
        setBackground(GROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR),
                BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));
        add(caption, BorderLayout.NORTH);
        add(rows, BorderLayout.CENTER);

        showNoSelection();
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Captions the panel "No cell selected" and shows {@value #BLANK} in every row.
     */
    public void showNoSelection() {
        caption.setText("No cell selected");
        clearValues();
    }

    /**
     * Captions the panel with the given cell followed by "no tile", and shows {@value #BLANK} in every row.
     *
     * @param cell the free cell
     * @throws IllegalArgumentException if the cell is null
     */
    public void showEmptyCell(Point cell) {
        requireCell(cell);

        caption.setText(String.format("%s, no tile", describeCell(cell)));
        clearValues();
    }

    /**
     * Captions the panel with the given cell, and shows each component of the given metadata in its own row.
     *
     * @param cell the cell that the metadata belongs to
     * @param data the metadata to show
     * @throws IllegalArgumentException if the cell or the data is null
     */
    public void showMetadata(Point cell, TileData data) {
        requireCell(cell);

        if (data == null) {
            throw new IllegalArgumentException("Shown metadata requires tile data");
        }

        caption.setText(describeCell(cell));
        traversable.setText(describeFlag(data.isTraversable()));
        shadow.setText(describeFlag(data.hasShadow()));
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Adds one row to the grid of rows: the component's name against the left edge, and its value against the
     * right.
     */
    private void addRow(JPanel rows, String name, JLabel value) {
        var label = new JLabel(name, SwingConstants.LEFT);

        styleLabel(label, NAME_COLOUR,  Font.PLAIN, NAME_SIZE);
        styleLabel(value, VALUE_COLOUR, Font.BOLD,  VALUE_SIZE);

        rows.add(label);
        rows.add(value);
    }

    private void styleLabel(JLabel label, Color colour, int weight, int size) {
        label.setForeground(colour);
        label.setFont(label.getFont().deriveFont(weight, (float) size));
    }

    private void clearValues() {
        traversable.setText(BLANK);
        shadow.setText(BLANK);
    }

    private String describeCell(Point cell) {
        return String.format("Row %d, column %d", cell.y, cell.x);
    }

    private String describeFlag(boolean flag) {
        return flag ? "Yes" : "No";
    }

    private void requireCell(Point cell) {
        if (cell == null) {
            throw new IllegalArgumentException("A shown cell requires a position");
        }
    }

}
