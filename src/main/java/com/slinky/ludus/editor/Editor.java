package com.slinky.ludus.editor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
    //                                           Fields                                           \\
    // ========================================================================================== \\

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    public Editor() {
        super("Ludus Level Editor");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        add(new RootPanel(TILESETS));
        pack();
        setLocationRelativeTo(null);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\

    // ========================================================================================== \\
    //                                          Setters                                           \\
    // ========================================================================================== \\

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\

    // ========================================================================================== \\
    //                                       Helper Classes                                       \\
    // ========================================================================================== \\
}
