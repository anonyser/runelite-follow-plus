# Follow Plus

Small quality-of-life plugin for Soul Wars taxiing and anywhere else you spend your time following
people around.

What it does:

- **Follow at the top** - right-clicking another player puts their Follow option at the top of the
  menu, above Attack / Trade / Walk here, so you don't fat-finger an attack when you only wanted a
  taxi. Nothing is removed from the menu, entries are just reordered.
- **Status window** - shows who you're following, your HP (plus a warning when you're being
  attacked), your active prayers with how long your prayer will last, and inside Soul Wars the
  activity bar with how long until it empties, the match time left, and in the lobby the number of
  players waiting and time to the next game. A green "In game" / red "Out of game" line sits at the
  top. It can show as an in-client overlay or as a separate always-on-top window that stays visible
  when RuneLite is minimised (handy for watching an alt). The external window is on by default.

There is no sidebar panel. Everything is configured from the normal plugin settings, and the
plugin's own on/off toggle is the usual RuneLite one. Values that can't be read safely show
"Unknown" rather than a guess.

The prayer time uses the same drain math as RuneLite's built-in prayer plugin (drain effect of each
active prayer vs 2x your equipment prayer bonus + 60), with the prayer bonus read from your worn
gear.

## Building

```
./gradlew clean test build
```

or double-click `build.bat` on Windows. `run-client.bat` starts a dev RuneLite client with the
plugin loaded.
