package com.slinky.ludus.editor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * The main window. Currently used as a scratch pad for displaying components as we build them
 * <p>
 * The frame is undecorated, so the caption comes from the {@code TitleBar} inside {@link RootPanel} and the
 * window paints every pixel a user sees.
 *
 * @author Kheagen Haskins
 * @version 1.0.0
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
