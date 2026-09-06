# Ludus Level Editor

**Status:** starting. **Last updated:** 6 September 2026.

## Purpose

Ludus loads a level from a JSON file, and this tool writes that file. The developer paints a grid of
tiles from a tileset image on screen, and the editor turns what they painted into the file the game
reads back.

The tool is a separate project because Ludus needs the file alone. Neither side imports code from
the other, so the game and the editor each move at their own pace.

## The output contract

The Ludus README specifies the level file format under its "Level file format" heading, and that
specification governs this project. A file this tool writes that Ludus refuses to load is a defect
here.

The format lives in one document on purpose, so the game and the editor read the same specification.
A copy here would be a second thing to keep in step.

Ludus loads a level from its own classpath at `/levels/<number>.json`, which is
`src/main/resources/levels/<number>.json` in a Ludus working copy. A save written there reaches the
game on its next build.

## The art

`src/main/resources/assets` contains the sprite set a level is drawn from. The ground layer comes
from `Terrain/Tileset/Tilemap_color1.png`, which divides into 64 pixel cells, and
`Terrain/Tileset/Water Background color.png` supplies the flat colour behind every cell a level
leaves unpainted. The Ludus README records the five `Tilemap_colorN.png` files as the art that
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

## The boundary

Ludus owns the game: the model types, the rendering, the simulation and the curriculum. This project
owns one file format and the tool that produces it. A question about how a fighter moves belongs in
Ludus, and a question about how a developer paints a tile belongs here.
