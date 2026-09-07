# Ludus Level Editor

**Status:** starting. **Last updated:** 7 September 2026.

## Purpose

Ludus loads a level from a JSON file, and this tool writes that file. The developer paints a grid of
tiles from a tileset image on screen, and the editor turns what they painted into the file the game
reads back.

The tool is a separate project because Ludus needs the file alone. Neither side imports code from
the other, so the game and the editor each move at their own pace.

## The level file format

This project writes the level file and Ludus reads it, so this section is the specification both
sides work to. It lives here because the tool that produces a format is the one that decides it. A
change to the format is a change to this document first, and Ludus follows.

A level is one JSON file. The file states its size and its tilesets once, then lists the painted
cells of each layer.

```json
{
  "rows": 10,
  "columns": 10,
  "tilesets": [
    {
      "path": "/assets/terrain/tilesets/tilemap_color1.png",
      "cellSize": 64
    }
  ],
  "layers": [
    {
      "tiles": [
        { "row": 8, "column": 3, "tileset": 0, "sourceRow": 1, "sourceColumn": 1 },
        { "row": 8, "column": 4, "tileset": 0, "sourceRow": 1, "sourceColumn": 1 }
      ]
    },
    {
      "tiles": [
        { "row": 8, "column": 4, "tileset": 0, "sourceRow": 5, "sourceColumn": 6 }
      ]
    }
  ]
}
```

The fields divide into the size of the grid, the tilesets, and the layers.

| Field                                          | Meaning                                              |
|------------------------------------------------|------------------------------------------------------|
| `rows`, `columns`                              | The size of the grid, which every layer shares       |
| `tilesets[].path`                              | The classpath location of one tileset image          |
| `tilesets[].cellSize`                          | The pixel width and height of one cell of that image |
| `layers[]`                                     | One entry per layer, bottom of the stack first       |
| `layers[].tiles[].row`, `.column`              | The cell of the level grid this tile occupies        |
| `layers[].tiles[].tileset`                     | An index into the `tilesets` list                    |
| `layers[].tiles[].sourceRow`, `.sourceColumn`  | The cell of that tileset the art comes from          |

### Layer order

A layer's position in the `layers` array is the order Ludus draws it in. Index 0 goes down first and
each layer after it draws over the one below.

A tile's transparent pixels let the layers beneath show through, and that is the point of the stack.
One cell of the example above stores grass on layer 0 and a cliff face on layer 1, which is how the
overhanging face of a plateau covers the ground behind it. A single grid per level could store only
the later of those two tiles.

### Empty layers

A file records every layer from index 0 up to the highest one containing a tile. An unpainted layer
between two painted ones appears with an empty `tiles` list, which keeps the index of every layer
above it equal to its draw order. Layers above the highest painted one go unwritten.

### Cells nobody painted

Each layer records an entry for the cells painted on it alone, so a mostly empty level produces a
short file. Ludus paints water behind layer 0 in the one flat colour taken from
`water-background-color.png`, and a cell absent from every layer shows that colour.

## Where a save goes

Ludus loads a level from its own classpath at `/levels/<number>.json`, which is
`src/main/resources/levels/<number>.json` in a Ludus working copy. A save written there reaches the
game on its next build.

## The art

`src/main/resources/assets` contains the sprite set a level is drawn from. The ground layer comes
from `terrain/tilesets/tilemap_color1.png`, which divides into 64 pixel cells, and
`terrain/tilesets/water-background-color.png` supplies the flat colour behind every cell a level
leaves unpainted. The Ludus README records the five `tilemap_colorN.png` files as the art that
elevation will draw from.

Ludus stores the same files. This is a decision: the art is maintained in Ludus and copied here, so
the game and the editor draw from one set of tiles.

## Environment

The build enforces Java 25 and Maven 3.8.1 or later. The interface is Swing, which ships inside the
JDK, so a machine that runs the build runs the editor.

| Command | Purpose |
|---|---|
| `mvn clean install -DskipTests` | Rebuild after a change |
| `mvn clean install` | Full build, with the test suite |
| `mvn test` | The test suite alone |

## The branches

The developer commits every change to `dev`, and that branch keeps each commit as it was made.

`main` records the stable iterations. The developer squashes a finished iteration from `dev` into a
single commit, so `main` gains one commit per version of the editor that runs.

The developer publishes an iteration with four commands:

```bash
git switch main
git merge --squash dev
git commit
git switch dev
```

## The boundary

Ludus owns the game: the model types, the rendering, the simulation and the curriculum. This project
owns one file format and the tool that produces it. A question about how a fighter moves belongs in
Ludus, and a question about how a developer paints a tile belongs here.
