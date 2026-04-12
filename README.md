# Literal Agenda

Your schedule, not theirs.

A minimalist text-based agenda app with Git sync.

<p>
  <a href="https://github.com/256x/agenda/releases/latest"><img src="https://img.shields.io/github/v/release/256x/agenda?label=GitHub%20Release"></a>
  <!-- &nbsp;<a href="https://apt.izzysoft.de/packages/fumi.day.literalagenda"><img src="https://img.shields.io/badge/IzzyOnDroid-download-brightgreen"></a> -->
  &nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

<p>
    <img width="180" alt="List Screen" src="https://github.com/user-attachments/assets/79f8142a-b97d-474a-9cbc-1347231642cf" />
    <img width="180" alt="Agenda Detail Screen" src="https://github.com/user-attachments/assets/07b4b531-89c1-4be4-b1e8-6d2dc5d6aadf" />
    <img width="180" alt="Calendar View Screen" src="https://github.com/user-attachments/assets/8ef89114-d8a3-403f-b2be-52c00fc9abe9" />
    <img width="180" alt="Setting Screen" src="https://github.com/user-attachments/assets/b36310e3-2ecb-4b17-93eb-ea4958d153eb" />
</p>


[User Guide](./docs/USER_GUIDE.md)

## Why?

Do you really need that much?

Google Calendar is powerful. Outlook is powerful. But how much of that power do you actually use for your personal life?

Your dentist appointment. A drink with a friend. A haircut next Tuesday.

Do those really need color-coded categories, shared calendars, and meeting invites?

And do you want Google to know your private schedule — in detail?

## The Idea

Literal Agenda is built around a simple observation:

**Work calendars are for work. Your private life deserves something quieter.**

No shared calendars. No meeting invites. No integrations.

Just your events, stored as plain text files, synced through a private Git repository you control.

## Features

- **'Literal' input**: type `15` for (current month) 15, `415` for April 15, `930` for 9:30
- **Mini calendar**: Optional month view with event indicators
- **Sync**: Git sync (GitHub, Gitea/Forgejo, Codeberg) to keep events across devices
- **Import**: Import from any iCal-compatible calendar app (.ics)
- **Repeat**: Weekly, monthly, and yearly repeat support
- **Search**: Filter events by title or note
- **Customize**: Font, size, accent color, controls on left

## How it works

Events are stored as plain text files in a simple directory:

```
repo/
├── events/      ← one file per event
├── repeating/   ← repeating events
└── trash/       ← deleted events (kept for recovery)
```

Each file is a plain text representation of a single event. No database. No proprietary format. Just files you can read, edit, or delete directly.

## Who is this for?

This app may appeal to people who:

- want to separate private and work schedules
- prefer plain text over cloud-managed data
- are comfortable with Git-based sync
- find mainstream calendar apps overwhelming for personal use
- migrated from Google Calendar and never looked back

## Import from Google Calendar

You can export your existing events from Google Calendar as an iCal file and import them directly into Literal Agenda.

See [User Guide](./docs/USER_GUIDE.md) for details.

## Philosophy

Literal Agenda is not a calendar app trying to do less.

It's a different idea of what a personal schedule should be.

If a feature requires explaining in multiple steps, it probably doesn't belong here.

## Development

- Kotlin / Jetpack Compose
- Target: Android 8.0+
- No Google APIs. No Firebase. No tracking.

This app was built with substantial assistance from [Claude](https://claude.ai) (Anthropic). AI was involved throughout development, including writing code.

## License

MIT

