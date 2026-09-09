package com.slinky.ludus.editor.data;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The five colours that the editor paints its chrome from, read from a classpath resource.
 * <p>
 * A palette file states one colour per line, in the fixed order {@code dark}, {@code light}, {@code accent1},
 * {@code accent2} and {@code accent3}. Each line takes the form {@code 0xRRGGBB}, {@code #RRGGBB} or a plain
 * decimal number, since {@link Integer#decode(String)} reads all three. A line starting with {@value #COMMENT}
 * describes the line below it, and a blank line separates one entry from the next.
 * <p>
 * Five colours cover more than five roles, so a caller derives the rest with {@link #withAlpha(Color, int)}: a
 * separator is {@link #getAccent3()} at a low alpha, and a hover is {@link #getAccent2()} at a low alpha.
 * <p>
 * <b>Painting a panel from a palette</b>
 * <pre>{@code
 * var palette = new Palette("/palettes/palette1.txt");
 *
 * panel.setBackground(palette.getDark());
 * panel.setForeground(palette.getLight());
 * panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.withAlpha(palette.getAccent3(), 40)));
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 2.0.0
 *          <p>
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public final class Palette {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The number of colours that a palette file states, one per line. */
    public static final int COLOUR_COUNT = 5;

    /** The prefix that marks a line as a description of the colour below it. */
    public static final String COMMENT = "//";

    /** The palette that {@link #getActive()} reads on the first call. */
    public static final String DEFAULT_RESOURCE = "/palettes/palette1.txt";

    /** How far {@link #getChrome()} travels from the dark tone towards the light one. */
    private static final float CHROME_LIFT = 0.07f;

    private static final int SEPARATOR_ALPHA = 26;
    private static final int GRID_ALPHA      = 70;

    private static Palette active;

    /**
     * Returns the palette that every component paints from, reading {@value #DEFAULT_RESOURCE} on the first
     * call. Swing runs on one thread, so the components share this instance without guarding it.
     *
     * @return the active palette
     * @throws IllegalArgumentException if {@value #DEFAULT_RESOURCE} states fewer than {@value #COLOUR_COUNT}
     *                                  colours, or if any line states something other than a colour
     */
    public static Palette getActive() {
        if (active == null) {
            active = new Palette(DEFAULT_RESOURCE);
        }

        return active;
    }

    /**
     * Replaces the palette that {@link #getActive()} returns. A component that read a colour into a static
     * field keeps the colour that it read, so a caller sets this before building the window.
     *
     * @param palette the palette to paint from
     * @throws IllegalArgumentException if the palette is null
     */
    public static void setActive(Palette palette) {
        if (palette == null) {
            throw new IllegalArgumentException("An active palette requires a palette");
        }

        active = palette;
    }

    /**
     * Returns the colour a given distance between two others, which is how a caller reaches a tone that neither
     * of the five names, such as the raised chrome between the ground and the text.
     *
     * @param from   the colour at 0
     * @param to     the colour at 1
     * @param amount the distance to travel, from 0 for {@code from} to 1 for {@code to}
     * @return a new opaque colour between the two
     * @throws IllegalArgumentException if either colour is null, or if the amount falls outside 0 to 1
     */
    public static Color blend(Color from, Color to, float amount) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("A blend requires two colours");
        }

        if (amount < 0f || amount > 1f) {
            throw new IllegalArgumentException(String.format("A blend amount must fall between 0 and 1, given %f", amount));
        }

        return new Color(
                Math.round(from.getRed()   + (to.getRed()   - from.getRed())   * amount),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
                Math.round(from.getBlue()  + (to.getBlue()  - from.getBlue())  * amount)
        );
    }

    /**
     * Returns one colour at a different opacity, which is how a caller reaches a role that the five named
     * colours leave to it, such as a separator or a hover fill.
     *
     * @param colour the colour to copy
     * @param alpha  the opacity to apply, from 0 for clear to 255 for solid
     * @return a new colour with the same red, green and blue
     * @throws IllegalArgumentException if the colour is null, or if the alpha falls outside 0 to 255
     */
    public static Color withAlpha(Color colour, int alpha) {
        if (colour == null) {
            throw new IllegalArgumentException("An alpha variant requires a colour");
        }

        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException(String.format("An alpha must fall between 0 and 255, given %d", alpha));
        }

        return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
    }

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final Color dark;
    private final Color light;
    private final Color accent1;
    private final Color accent2;
    private final Color accent3;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Reads a palette from the classpath.
     *
     * @param filename the resource to read, starting at the classpath root
     * @throws IllegalArgumentException if the classpath contains no resource at that path, if the resource
     *                                  states fewer than {@value #COLOUR_COUNT} colours, or if any line states
     *                                  something other than a colour
     * @throws UncheckedIOException     if reading the resource fails
     */
    public Palette(String filename) {
        var source = Palette.class.getResourceAsStream(filename);

        if (source == null) {
            throw new IllegalArgumentException(String.format("The classpath contains no resource at '%s'", filename));
        }

        try (source) {
            var colours = readColours(source, filename);

            this.dark    = colours.get(0);
            this.light   = colours.get(1);
            this.accent1 = colours.get(2);
            this.accent2 = colours.get(3);
            this.accent3 = colours.get(4);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to read the palette at '%s'", filename), e);
        }
    }

    /**
     * Builds a palette from five colours already in memory.
     *
     * @param dark    the deepest tone, which the window grounds itself in
     * @param light   the lightest tone, which text and glyphs paint in
     * @param accent1 the colour of a selection
     * @param accent2 the colour of a hover, a focus, and the value that a stepper stands on
     * @param accent3 the colour of the save action, and of a control's outline under the pointer
     * @throws IllegalArgumentException if any colour is null
     */
    public Palette(Color dark, Color light, Color accent1, Color accent2, Color accent3) {
        if (dark == null || light == null || accent1 == null || accent2 == null || accent3 == null) {
            throw new IllegalArgumentException("A palette requires all five colours");
        }

        this.dark    = dark;
        this.light   = light;
        this.accent1 = accent1;
        this.accent2 = accent2;
        this.accent3 = accent3;
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    /** Returns the deepest tone, which the window grounds itself in. */
    public Color getDark() {
        return dark;
    }

    /** Returns the lightest tone, which text and glyphs paint in. */
    public Color getLight() {
        return light;
    }

    /** Returns the colour of a selection. */
    public Color getAccent1() {
        return accent1;
    }

    /** Returns the colour of a hover, a focus, and the value that a stepper stands on. */
    public Color getAccent2() {
        return accent2;
    }

    /** Returns the colour of the save action, and of a control's outline under the pointer. */
    public Color getAccent3() {
        return accent3;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Returns the raised surface that the title bar and the control bar fill with, a step from
     * {@link #getDark()} towards {@link #getLight()}. The step is small, so the chrome separates from the
     * window ground and stays darker than the tile art on the canvas.
     *
     * @return an opaque colour just lighter than {@link #getDark()}
     */
    public Color getChrome() {
        return blend(dark, light, CHROME_LIFT);
    }

    /**
     * Returns the line that divides one strip of chrome from the canvas, which is {@link #getLight()} at a low
     * opacity so it lifts off whatever fills the space behind it.
     *
     * @return a translucent colour
     */
    public Color getSeparator() {
        return withAlpha(light, SEPARATOR_ALPHA);
    }

    /**
     * Returns the line that divides one cell from the next, on the canvas and on a swatch alike. Both draw
     * their grid over tile art, so this darkens rather than lightens and stays legible over the brightest
     * pixels in a tileset.
     *
     * @return a translucent colour
     */
    public Color getGridLine() {
        return withAlpha(dark, GRID_ALPHA);
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Reads every colour line of a palette resource, passing over blank lines and comments.
     *
     * @throws IllegalArgumentException if the resource states fewer than {@value #COLOUR_COUNT} colours
     */
    private static List<Color> readColours(InputStream source, String filename) throws IOException {
        var text  = new String(source.readAllBytes(), StandardCharsets.UTF_8);
        var found = new ArrayList<Color>(COLOUR_COUNT);

        // "\\R" matches every line break, so a file written on Windows parses the same as one written on Linux
        for (var line : text.split("\\R")) {
            var entry = line.trim();

            if (!entry.isEmpty() && !entry.startsWith(COMMENT)) {
                found.add(parseColour(entry, filename));
            }
        }

        if (found.size() < COLOUR_COUNT) {
            throw new IllegalArgumentException(String.format(
                    "A palette requires %d colours, and '%s' states %d", COLOUR_COUNT, filename, found.size()
            ));
        }

        return found;
    }

    /**
     * Converts one line to a colour, reading {@code 0xRRGGBB}, {@code #RRGGBB} and plain decimal alike.
     *
     * @throws IllegalArgumentException if the line states something other than a colour
     */
    private static Color parseColour(String entry, String filename) {
        try {
            return new Color(Integer.decode(entry), false);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(String.format(
                    "The palette at '%s' states '%s', where a colour such as 0xRRGGBB belongs", filename, entry
            ), ex);
        }
    }

}
