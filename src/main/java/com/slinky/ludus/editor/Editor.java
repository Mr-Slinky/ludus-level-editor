package com.slinky.ludus.editor;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The application window, which opens a {@link RootPanel} over the terrain tilesets and centres itself on the
 * screen.
 * <p>
 * The frame is undecorated, so the caption comes from the {@link com.slinky.ludus.editor.panels.TitleBar
 * TitleBar} inside the root panel, and the window paints every pixel that a user sees.
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
 *          Last modified: 2026-09-08
 * @since 1.0.0
 */
public class Editor extends JFrame {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final String ICON_RESOURCE  = "/icons/favicon-%d.png";
    private static final int[]  ICON_SIZES     = {16, 32, 48, 128, 256, 512};

    private static final String[] TILESETS = {
            "terrain/tilesets/tilemap_color1.png",
            "terrain/tilesets/tilemap_color2.png",
            "terrain/tilesets/tilemap_color3.png",
            "terrain/tilesets/tilemap_color4.png",
            "terrain/tilesets/tilemap_color5.png"
    };

    static void main() {
        SwingUtilities.invokeLater(() -> new Editor().setVisible(true));
    }

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
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
    // Skips any size absent from the classpath, so ICON_SIZES may list more than ships.
    private static List<Image> loadIcons() {
        var icons = new ArrayList<Image>();

        for (int size : ICON_SIZES) {
            var resource = ICON_RESOURCE.formatted(size);
            try (var in = Editor.class.getResourceAsStream(resource)) {
                if (in != null) {
                    icons.add(ImageIO.read(in));
                }
            } catch (IOException ex) {
                throw new IllegalStateException(String.format("Icon resource '%s' failed to decode", resource), ex);
            }
        }

        return icons;
    }
}
