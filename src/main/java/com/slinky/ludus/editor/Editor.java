package com.slinky.ludus.editor;

import com.slinky.ludus.editor.panels.SwatchPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * The main window. Currently used as a scratch pad for displaying components as we build them
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 * @since 1.0.0
 */
public class Editor extends JFrame {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final int TILE_SIZE = 64;

    private static final String[] TILESETS = {
            "terrain/tilesets/Tilemap_color1.png",
            "terrain/tilesets/Tilemap_color2.png",
            "terrain/tilesets/Tilemap_color3.png",
            "terrain/tilesets/Tilemap_color4.png",
            "terrain/tilesets/Tilemap_color5.png"
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
        add(new SwatchPanel(TILE_SIZE, TILESETS));
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
