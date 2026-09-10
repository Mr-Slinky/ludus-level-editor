package com.slinky.ludus.editor;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The application window. The frame opens a {@link RootPanel} over the terrain tilesets, sizes itself to that
 * panel, and centres itself on the screen.
 * <p>
 * The frame is undecorated, so the caption comes from the {@link com.slinky.ludus.editor.panels.TitleBar
 * TitleBar} inside the root panel, and every pixel of the window comes from the components inside it. Closing
 * the window ends the process.
 * <p>
 * <b>Opening the editor</b>
 * <p>
 * A caller shows the window on the event dispatch thread:
 * <pre>{@code
 * SwingUtilities.invokeLater(() -> new Editor().setVisible(true));
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-10
 * @since 1.0.0
 */
public class Editor extends JFrame {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    /** The icon resource for one size, with {@code %d} taking that size in pixels. */
    private static final String ICON_RESOURCE  = "/icons/favicon-%d.png";

    /** The sizes to load, so the desktop picks the one that fits where it draws the icon. */
    private static final int[]  ICON_SIZES     = {16, 32, 48, 128, 256, 512};

    /** The tilesets that the editor opens over, in the form that {@link RootPanel#RootPanel(String...)} takes. */
    private static final String[] TILESETS = {
            "terrain/tilesets/tilemap_color1.png",
            "terrain/tilesets/tilemap_color2.png",
            "terrain/tilesets/tilemap_color3.png",
            "terrain/tilesets/tilemap_color4.png",
            "terrain/tilesets/tilemap_color5.png"
    };

    /**
     * Opens the editor on the event dispatch thread.
     */
    static void main() {
        SwingUtilities.invokeLater(() -> new Editor().setVisible(true));
    }

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds the window at the size that {@link RootPanel} asks for, centred on the screen. A caller shows it
     * with {@link #setVisible(boolean)}.
     *
     * @throws IllegalStateException if an icon file is present and fails to decode
     */
    public Editor() {
        super("Ludus Level Editor");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(loadIcons());
        setUndecorated(true);
        add(new RootPanel(TILESETS));
        pack();
        setLocationRelativeTo(null);
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Reads the window icon at every size that {@link #ICON_SIZES} lists, taking each from
     * {@value #ICON_RESOURCE} on the classpath.
     *
     * @return one image per size that the classpath supplies a file for, in the order that {@code ICON_SIZES}
     *         lists them
     * @throws IllegalStateException if a file is present and fails to decode
     */
    private static List<Image> loadIcons() {
        var icons = new ArrayList<Image>();

        for (int size : ICON_SIZES) {
            var resource = ICON_RESOURCE.formatted(size);
            try (var in = Editor.class.getResourceAsStream(resource)) {
                if (in != null) {
                    icons.add(ImageIO.read(in));
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Icon resource '%s' failed to decode".formatted(resource), ex);
            }
        }

        return icons;
    }

}
