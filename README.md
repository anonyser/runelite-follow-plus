# Follow Plus

Small quality-of-life plugin for Soul Wars taxiing and anywhere else you spend your time following
people around.

What it does:

- **Follow at the top** - right-clicking another player puts their Follow option at the top of the
  menu, above Attack / Trade / Walk here, so you don't fat-finger an attack when you only wanted a
  taxi. Nothing is removed from the menu, entries are just reordered.
- **Status overlay** - a small movable overlay showing who you're following, your HP (plus a
  warning when you're being attacked), your active prayers with an estimated time until your
  prayer points run out, and inside Soul Wars the activity bar with an estimated time until it
  empties and the match time remaining.

There is no sidebar panel. Everything is configured from the normal plugin settings, and the
plugin's own on/off toggle is the usual RuneLite one. Turn on "Debug logging" if a Soul Wars value
shows as Unknown and check the client log - the plugin logs what it sees so the right widget can
be pinned down.

The prayer estimate uses the same drain math as RuneLite's built-in prayer plugin (drain effect of
each active prayer vs 2x your equipment prayer bonus + 60), with the prayer bonus read from your
worn gear.

## Building

```
./gradlew clean test build
```

or double-click `build.bat` on Windows. `run-client.bat` starts a dev RuneLite client with the
plugin loaded.
