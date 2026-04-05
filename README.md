# Literal Agenda

Your schedule, not theirs.

A minimalist text-based agenda app with Git sync.

<p>
  <a href="https://github.com/256x/agenda/releases/latest"><img src="https://img.shields.io/github/v/release/256x/agenda?label=GitHub%20Release"></a>&nbsp;<a href="https://apt.izzysoft.de/packages/fumi.day.literalagenda"><img src="https://img.shields.io/badge/IzzyOnDroid-download-brightgreen"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
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

No recurring meeting management. No invites. No integrations.

Just your events, stored as plain text files, synced through a private GitHub repository you control.

## Features

- **'Literal' input**: type `15` for (current month) 15, `415` for April 15, `930` for 9:30
- **Mini calendar**: Optional month view with event indicators
- **Sync**: GitHub sync to keep events across devices
- **Import**: Import from any iCal-compatible calendar app (.ics)
- **Repeat**: Daily, weekly, monthly, and yearly repeat support
- **Search**: Filter events by title or note
- **Customize**: Font, size, accent color, controls on left

## How it works

Events are stored as plain text files in a simple directory:

```
repo/
└── events/    ← one file per event
```

Each file is a plain text representation of a single event. No database. No proprietary format. Just files you can read, edit, or delete directly.

## Why not Google Calendar?

Because you don't need it for your personal life.

Google Calendar is excellent for work — shared schedules, meeting rooms, video call links. Use it there.

But for your private life, it's overkill. And you're handing your daily routine to a third party.

Literal Agenda gives you a private, readable, searchable calendar that lives in a Git repo you own.

## Who is this for?

This app may appeal to people who:

- want to separate private and work schedules
- prefer plain text over cloud-managed data
- are comfortable with GitHub as a sync backend
- find mainstream calendar apps overwhelming for personal use
- migrated from Google Calendar and never looked back

## Import from Google Calendar

You can export your existing events from Google Calendar as an iCal file and import them directly into Literal Agenda.

See [User Guide](./docs/USER_GUIDE.md) for details.

## Philosophy

Literal Agenda is not a calendar app trying to do less.

It's a different idea of what a personal schedule should be.

## Design Constraints

This app is intentionally simple.

If a feature requires explaining in multiple steps, it probably doesn't belong here.

## Development

Built with Kotlin and Jetpack Compose.

- Kotlin / Jetpack Compose
- Target: Android 8.0+
- No Google APIs. No Firebase. No tracking.

## License

MIT

