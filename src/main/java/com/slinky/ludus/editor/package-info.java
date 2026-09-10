/**
 * The window and the pieces that assemble it.
 * <p>
 * {@link com.slinky.ludus.editor.Editor} is the frame that a user launches, {@link RootPanel} is the content
 * inside it, and {@code LevelWriter} converts what a user painted into the JSON that Ludus loads. The root
 * panel owns the cell size and the grid maximum, and wires every control to the canvas, so the packages below
 * this one are reached through it.
 */
package com.slinky.ludus.editor;
