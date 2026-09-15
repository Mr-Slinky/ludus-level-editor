/**
 * The level as data, apart from the components that draw it.
 * <p>
 * {@link com.slinky.ludus.editor.data.TileGrid} stores the tiles and the metadata of one layer,
 * {@link TileSource} and {@link TileSet} record where a tile's art was cut from, and {@link TileData} records
 * how a tile behaves once Ludus loads it. {@link JsonUtil} converts a set of layers to the JSON of a level
 * file, which is the contract between this editor and the game.
 * <p>
 * {@link Palette} serves the components rather than the level, and reads the five colours that all of them
 * paint from.
 */
package com.slinky.ludus.editor.data;
