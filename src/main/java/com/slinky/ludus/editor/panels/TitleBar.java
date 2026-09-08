package com.slinky.ludus.editor.panels;

import com.slinky.ludus.editor.components.TitleBarButton;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The strip that replaces the system caption on an undecorated window, with a minimise, a maximise and a close
 * button along its right edge. Dragging anywhere else on the strip moves the window, which is the gesture that
 * the system caption would otherwise provide.
 * <p>
 * The bar reaches its window through {@link SwingUtilities#getWindowAncestor(Component)} at the moment that a
 * button is pressed, so a caller adds the bar to any window without passing that window in. Closing dispatches
 * {@link WindowEvent#WINDOW_CLOSING} rather than ending the process, so the window's own
 * {@code setDefaultCloseOperation} and every registered {@code WindowListener} run as they do under the system
 * caption.
 * <p>
 * Two areas stay free for a caller to fill: {@link #addLeading(JComponent)} places a component against the left
 * edge, and {@link #addTrailing(JComponent)} places one immediately left of the three window buttons.
 * <p>
 * <b>Giving an undecorated frame a caption</b>
 * <p>
 * A caller builds the bar, captions it, and adds it along the top of a frame that paints its own decoration:
 * <pre>{@code
 * var bar = new TitleBar();
 * bar.addLeading(new JLabel("Ludus Level Editor"));
 *
 * frame.setUndecorated(true);
 * frame.add(bar, BorderLayout.NORTH);
 *
 * // the bar reports a preferred height of 32 pixels, the height of one caption button
 * }</pre>
 *
 * @author Kheagen Haskins
 * @version 1.0.0
 *          <p>
 *          Last modified: 2026-09-07
 * @since 1.0.0
 */
public class TitleBar extends JPanel {

    // ========================================================================================== \\
    //                                           Static                                           \\
    // ========================================================================================== \\
    private static final Color BACKGROUND = new Color(243, 243, 243);
    private static final Color SEPARATOR  = new Color(0, 0, 0, 30);

    // ========================================================================================== \\
    //                                           Fields                                           \\
    // ========================================================================================== \\
    private final TitleBarButton minimise = new TitleBarButton(TitleBarButton.Glyph.MINIMISE);
    private final TitleBarButton maximise = new TitleBarButton(TitleBarButton.Glyph.MAXIMISE);
    private final TitleBarButton close    = new TitleBarButton(TitleBarButton.Glyph.CLOSE);

    private final JPanel leading  = new JPanel(new FlowLayout(FlowLayout.LEFT,  0, 0));
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    /** The distance from the window's top left corner to the point at which a drag started, in screen pixels. */
    private Point grabOffset;

    // ========================================================================================== \\
    //                                       Constructor(s)                                       \\
    // ========================================================================================== \\
    /**
     * Builds a bar carrying the three window buttons, with the space to their left free.
     */
    public TitleBar() {
        minimise.addActionListener(_ -> minimiseWindow());
        maximise.addActionListener(_ -> toggleMaximised());
        close.addActionListener(_ -> closeWindow());

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SEPARATOR));
        add(leading, BorderLayout.WEST);
        add(buildControlArea(), BorderLayout.EAST);

        installDragging(this);
        installDragging(leading);
        installDragging(trailing);
    }

    // ========================================================================================== \\
    //                                          Getters                                           \\
    // ========================================================================================== \\
    public TitleBarButton getMinimiseButton() {
        return minimise;
    }

    public TitleBarButton getMaximiseButton() {
        return maximise;
    }

    public TitleBarButton getCloseButton() {
        return close;
    }

    // ========================================================================================== \\
    //                                        API Methods                                         \\
    // ========================================================================================== \\
    /**
     * Adds a component against the left edge of the bar, to the right of anything already added there.
     *
     * @param component the component to add
     */
    public void addLeading(JComponent component) {
        leading.add(component);
        installDragging(component);
        revalidate();
    }

    /**
     * Adds a component immediately left of the three window buttons, to the right of anything already added
     * there.
     *
     * @param component the component to add
     */
    public void addTrailing(JComponent component) {
        trailing.add(component);
        installDragging(component);
        revalidate();
    }

    /**
     * Sends the window to the taskbar, leaving its size and position for the user to return to.
     */
    public void minimiseWindow() {
        findFrame().ifPresent(frame -> frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED));
    }

    /**
     * Grows the window to fill the screen, or returns a filled window to the size that it had before, and swaps
     * the middle button's glyph to match the state that the window ends in.
     * <p>
     * The maximised bounds come from {@link GraphicsEnvironment#getMaximumWindowBounds()}, which stops at the
     * taskbar. An undecorated window otherwise grows over it.
     */
    public void toggleMaximised() {
        findFrame().ifPresent(frame -> {
            if (isMaximised(frame)) {
                frame.setExtendedState(Frame.NORMAL);
                maximise.setGlyph(TitleBarButton.Glyph.MAXIMISE);
            } else {
                frame.setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                maximise.setGlyph(TitleBarButton.Glyph.RESTORE);
            }
        });
    }

    /**
     * Asks the window to close by dispatching {@link WindowEvent#WINDOW_CLOSING} to it, which gives every
     * registered {@code WindowListener} its say before the window goes.
     */
    public void closeWindow() {
        var window = SwingUtilities.getWindowAncestor(this);

        if (window != null) {
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        }
    }

    // ========================================================================================== \\
    //                                       Helper Methods                                       \\
    // ========================================================================================== \\
    /**
     * Lays the three window buttons out in a row against the right edge, with the free trailing area to their
     * left.
     */
    private JPanel buildControlArea() {
        trailing.setOpaque(false);
        leading.setOpaque(false);

        var controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);
        controls.add(trailing);
        controls.add(minimise);
        controls.add(maximise);
        controls.add(close);

        return controls;
    }

    /**
     * Makes a drag on the given component move the window. Swing delivers a mouse event to the deepest component
     * under the pointer alone, so every child that a user can grab receives its own copy of this listener.
     */
    private void installDragging(JComponent component) {
        var dragging = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                var window = SwingUtilities.getWindowAncestor(TitleBar.this);
                grabOffset = window == null ? null : new Point(e.getXOnScreen() - window.getX(), e.getYOnScreen() - window.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragWindowTo(e.getXOnScreen(), e.getYOnScreen());
            }

        };

        component.addMouseListener(dragging);
        component.addMouseMotionListener(dragging);
    }

    /**
     * Moves the window so that the point at which the drag started stays under the pointer. A maximised window
     * stays where it is, since it occupies the whole screen already.
     */
    private void dragWindowTo(int pointerX, int pointerY) {
        var window = SwingUtilities.getWindowAncestor(this);

        if (window == null || grabOffset == null || isMaximised(window)) {
            return;
        }

        window.setLocation(pointerX - grabOffset.x, pointerY - grabOffset.y);
    }

    /**
     * Returns the window that this bar belongs to, as a {@link Frame}, which is the type that declares the
     * extended state that the minimise and maximise buttons set.
     *
     * @return the ancestor frame, and empty while the bar belongs to no window or to a window of another type
     */
    private Optional<Frame> findFrame() {
        return SwingUtilities.getWindowAncestor(this) instanceof Frame frame ? Optional.of(frame) : Optional.empty();
    }

    /**
     * Reports whether the given window fills the screen.
     *
     * @param window the window to test
     * @return {@code true} while the window is a frame whose extended state includes {@code MAXIMIZED_BOTH}
     */
    private boolean isMaximised(Window window) {
        return window instanceof Frame frame && (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

}
