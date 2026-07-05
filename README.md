# Soul Wars Status

Small status overlay for Soul Wars taxiing and anywhere else you spend your time following people
around.

What it shows:

- **Status window** - who you're following, your HP (plus a warning when you're being attacked),
  your active prayers with how long your prayer will last, and inside Soul Wars the activity bar
  with how long until it empties, the match time left, and in the lobby the number of players
  waiting and time to the next game. A green "In game" / red "Out of game" line sits at the top. It
  can show as an in-client overlay or as a separate always-on-top window that stays visible when
  RuneLite is minimised (handy for watching an alt). The external window is on by default.

Following status is read-only: it notices when you follow another player and shows their name. The
plugin does not change or reorder any menu options. There is no sidebar panel; everything is in the
normal plugin settings. Values that can't be read safely show "Unknown" rather than a guess.

The prayer time uses the same drain math as RuneLite's built-in prayer plugin (drain effect of each
active prayer vs 2x your equipment prayer bonus + 60), with the prayer bonus read from your worn
gear.

## Building

```
./gradlew clean test build
```

or double-click `build.bat` on Windows. `run-client.bat` starts a dev RuneLite client with the
plugin loaded.
