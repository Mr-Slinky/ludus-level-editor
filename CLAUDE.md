# CLAUDE.md

How the developer and Claude Code work together in the Ludus level editor.

The global `~/.claude/CLAUDE.md` states the environment, the build commands, the naming and structure
conventions, the testing standards and the writing rules, and all of them apply here. This file adds
what is true of this project alone.

---

## What this project is

A desktop tool, written in Swing, that a single developer uses to draw a level and write it to a
JSON file. Ludus loads that file. The tool has one user and runs on one machine, so the work stops
at something that runs locally.

---

## The one constraint

The output format is the whole contract, and the Ludus README specifies it. Every other decision
here is open: the class structure, the interaction model, the layout, the number of panels, the way
a tileset is cut up.

Where a change would alter the shape of the file, the developer takes that decision and Ludus is
updated to match. Where a change affects the tool alone, the agent proceeds.

---

## Working posture

This project is a tool rather than the product, so delegation runs further here than it does in
Ludus. The developer reviews the file the editor writes and the tool on screen, in place of every
line that produces them.

Two things follow.

- **A task can arrive as an outcome.** "Make the palette scale with the window" is enough to act on,
  where the same instruction in Ludus would need decomposition first.
- **The review target is behaviour.** Opening the written file in a text editor, and looking at the
  grid on screen, settle whether a change worked.

The one place the Ludus posture returns is the file format. A change there costs the game a matching
change, so it is the developer's call every time.

---

## The unit of work

The agent writes one unit of work at a time. Each of these is one unit:

- A class together with its fields.
- The static constants of a class.
- A constructor.
- A record.

Two things follow for documentation.

- **Documentation is a unit of its own.** The agent writes a class in one unit, and its Javadoc and
  comments in a later one.
- **One file is documented per unit.** The developer asks by file where a unit is to cover more than
  one.

---

## The branch a change lands on

Every commit the agent makes goes on `dev`, and the README describes what each branch contains.

The developer declares an iteration stable by squashing it onto `main`, and that judgement is theirs
every time, exactly as the file format decision is. The agent commits to `main` when the developer
asks for the squash by name.

---

## Verification without a person watching

A Swing component paints into a `BufferedImage` off screen, which is how a change to the drawing is
checked inside a build:

```java
component.setSize(component.getPreferredSize());
var image  = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
var canvas = image.createGraphics();
component.printAll(canvas);
canvas.dispose();
ImageIO.write(image, "png", target);
```

A `JFrame` needs `addNotify()` and `validate()` before its layered pane paints this way, and a
component that installs a `DropTarget` needs a display, so a test that builds one skips itself
through `assumeFalse(GraphicsEnvironment.isHeadless())`.

Mouse gestures go the same way. A test dispatches a `MouseEvent` straight at the component and then
asserts on the level that came out, so the gesture handling runs inside the build like any other
code.

---

## The art

`src/main/resources/assets` is a copy of the Ludus asset tree. An edit to a sprite is made in Ludus
and copied here, so a change made here alone is lost at the next copy.

---

## Levels for testing

A level file written during development goes anywhere convenient. A level meant for the game goes
into `src/main/resources/levels` in a Ludus working copy, which is the directory Ludus loads from.
