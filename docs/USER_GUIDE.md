# Literal Agenda User Guide

## Getting Started

Literal Agenda is a minimalist personal agenda app.

**Philosophy**: Your schedule, not theirs.

No shared calendars. No meeting invites. Just your events, stored as plain text, synced through a private Git repository.

---

## Basic Usage

### Creating an Event

1. Tap the **+** button
2. Enter a date, optionally a time, and a title
3. Tap **Save**

### Editing an Event

1. Tap an event from the list to open the detail view
2. Tap the **Edit** button (pencil icon, bottom right)
3. Edit the fields
4. Tap **Save**

### Deleting an Event

1. Open an event (tap it from the list)
2. Tap the **trash icon** in the top bar
3. Confirm deletion

---

## Date and Time Input

Literal Agenda uses a numeric shorthand system. Tap the Date or Time field to open the numpad.

### Date

| Input | Interpreted as |
|-------|----------------|
| `15` | 15th of current month |
| `415` | April 15 |
| `0415` | April 15 |
| `260415` | April 15, 2026 |
| `20260415` | April 15, 2026 |

### Time

| Input | Interpreted as |
|-------|----------------|
| `9` | 9:00 |
| `14` | 14:00 |
| `930` | 9:30 |
| `1430` | 14:30 |

Time is optional. Leave it blank for all-day events.

---

## Search

Type in the search bar at the bottom of the main screen to filter events by title or note. Press Enter (Search) to apply. Press Back to clear the search.

Search covers **all events including past ones** — useful for looking up when something last happened.

---

## Mini Calendar

Tap the calendar icon in the top bar to toggle the mini calendar.

- Dates with events are highlighted
- Tap a date to see its events — including past dates
- Tap an event in the popup to open it

---

## Repeat

When creating or editing an event, you can set a repeat type:

- **Weekly** / **Monthly** / **Yearly**

---

## Git Sync

Supports **GitHub**, **Gitea/Forgejo** (including Codeberg), and any compatible Git forge.

### Setup (GitHub)

1. Create a **private** repository on GitHub (e.g., `username/agenda`)
2. Create a Personal Access Token:
   - Go to GitHub → Settings → Developer settings → Personal access tokens
   - Generate a token with `repo` scope
3. In Literal Agenda, go to **Settings**
4. Select **GitHub**, enter your token and repository (format: `username/repo`)
5. Tap **Connect**

### Setup (Gitea / Forgejo / Codeberg)

1. Create a **private** repository on your Gitea/Forgejo instance (e.g., Codeberg)
2. Create an access token in your account settings
3. In Literal Agenda, go to **Settings**
4. Select **Gitea/Forgejo**, enter the host URL (e.g., `codeberg.org`), token, and repository
5. Tap **Connect**

### How Sync Works

- Syncs automatically on app launch and each time the app returns to the foreground
- Manual sync available via the sync icon in the top bar
- Each event is stored as a plain text file in your repository

### Switching Repositories

If you change the forge, host, or repository, all local data is cleared on the next sync. The app will then download everything from the new repository. A warning is shown before this happens.

### Deleted Events

When you delete an event, it is moved to a `trash/` folder in your repository instead of being permanently deleted. The app never reads from `trash/` — it is purely a safety net on the remote side.

To permanently delete trashed events, remove the files from `trash/` directly in your repository and sync.

### Multi-device Usage

1. Set up Git Sync on all devices using the same repository
2. Let the app sync before editing
3. Avoid editing the same event on two devices simultaneously

### Conflict Resolution

If the same event is edited on two devices before syncing, the remote version wins. The local version is saved as a separate file (`filename_conflict_YYYYMMDD_HHmmss.md`) on the device, so no changes are lost.

---

## Import from Google Calendar

You can import your existing Google Calendar events into Literal Agenda.

### Export from Google Calendar

1. Open Google Calendar on desktop
2. Go to Settings → Import & Export
3. Select the calendar you want to export
4. Click **Export** — this downloads a `.ics` file

### Import into Literal Agenda

1. Transfer the `.ics` file to your Android device
2. In Literal Agenda, go to **Settings → Import**
3. Tap **Import from iCal (.ics)**
4. Select the file

Imported events will appear in your list immediately.

---

## Settings

| Setting | Description |
|---------|-------------|
| Font | Default, Serif, Mono, Scope |
| Size | Text size slider |
| Date format | Order of day, month, weekday |
| Colors | Background, text, accent |
| Controls on left | Moves action buttons (FAB) to left side on all screens |
| Git Sync | Connect your private repository (GitHub, Gitea/Forgejo, Codeberg) |
| Import | Import from iCal (.ics) |

---

## Troubleshooting

### Sync not working?

- Check your internet connection
- Verify your token has the required scope (`repo` for GitHub, appropriate permissions for Gitea/Forgejo)
- Make sure the repository exists
- For Gitea/Forgejo, verify the host URL is correct
- Try reconnecting in Settings

### Events not appearing on another device?

- Wait a few seconds after launch for sync to complete
- Check if the files exist in your repository
- Tap the sync icon to trigger a manual sync

### Lost an event?

Deleted events are moved to the `trash/` folder in your repository. To restore one, move the file from `trash/` back to `events/` (or `repeating/` for repeating events) directly in your repository, then sync the app.

