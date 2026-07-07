# Soul Wars Status

A companion overlay for Soul Wars, whether you're taxiing or just playing games.

It shows a status window, either as an in-client overlay or a separate always-on-top window that
stays visible when RuneLite is minimised (handy for watching an alt, and you can click it to bring
the game back to the front):

- who you're following, and whether they're moving
- your HP, plus a line for when you're fighting the Avatar with its health
- your active prayers and how long your prayer will last, coloured by how much is left
- your team colour, blue or red
- zeal: tokens, session, last game, lifetime, and per hour
- run energy
- your session win/loss record, with a red/blue team breakdown
- while in a game: your captures, avatar damage, avatar kills, fragments sacrificed, the activity
  bar (green, then red as it drops), and the match time left
- in the lobby: players waiting and time to the next game

Low activity, low prayer, and the last five minutes of a game flash so they catch your eye. Every
line can be turned off in the settings.

The side panel has a zeal calculator (work out the zeal for a target level or XP on any of the seven
trainable skills, filled in from your own levels), how many Spoils of War crates your tokens buy, and
a daily zeal-xp tracker against the 1,000,000 cap.

The plugin is display-only. It reads your following status (it doesn't change or reorder any menu
options) and the match stats the game already shows, and it never automates anything. Values it
can't read safely show "Unknown" rather than a guess.

## Building

```
./gradlew clean test build
```

or double-click `build.bat` on Windows. `run-client.bat` starts a dev RuneLite client with the
plugin loaded.
