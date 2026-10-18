# CCMM

This page describes the web client and Socket.IO server on git tag `legacy-web-v1`. Those directories are not on `master`. The desktop IDE on this branch does not start them.

To run the web app beside this checkout:

```sh
git worktree add ../ccmm-legacy legacy-web-v1
```

Paths below are relative to that checkout. Metric numbers come from [CCMM-Metrics-API](CCMM-Metrics-API.md), which is a separate process.

## Client

React 18, TypeScript, Vite, and Tailwind. Routes are declared in `CCMM/client/src/App.tsx`.

| Path | Screen |
|---|---|
| `/` | Room form. Username at least 3 characters, room id at least 5. "Create room" fills a new UUID. |
| `/home` | Feature overview |
| `/halsteadmetriccalc` | Posts the editor text as `text/plain` to `POST /calculate-halstead`. Shows vocabulary, length, volume, difficulty, and effort. |
| `/loccal` | Posts `{ "code": "..." }` to `POST /calculate-line-count`. Shows code, comment, blank, and method counts. |
| `/maintainabilitycal` | Posts the editor text as `text/plain` to `POST /calculate-maintainability-index`. Colors the rounded index: 0–9 red, 10–19 yellow, 20–100 green. |
| `/complexitycal` | Posts `{ "code": "..." }` to `POST /calculate-loop-depth`. Shows one integer. |
| `/editor/:roomId` | CodeMirror editor, file tree, tabs, settings, run panel, drawing canvas, chat, and presence. |

The complexity sidebar in the editor posts the active file to the maintainability and loop-depth endpoints on each content change.

`CCMM/client/src/test/` holds copies of calculator screens. It is not an automated test suite.

### Scripts

From `CCMM/client` in the legacy checkout:

| Script | Command |
|---|---|
| Dev server | `npm run dev` |
| Production build | `npm run build` |
| Lint | `npm run lint` |
| Preview the build | `npm run preview` |

Create `CCMM/client/.env` before `npm run dev`:

```
VITE_BACKEND_URL=http://localhost:3000
```

The socket client reads that variable in `src/context/SocketContext.tsx`. Metric calls are hardcoded to `http://localhost:8080`. Vite prints the client URL, usually `http://localhost:5173`.

The run panel executes code through the public Piston API at `https://emkc.org/api/v2/piston` (`src/api/index.tsx`). That call does not go through the metrics API.

## Server

Express and Socket.IO in `CCMM/server`. One in-memory room per `roomId`. There is no login. Usernames must be unique inside a room. Disconnect removes the user from the room.

From `CCMM/server`:

| Script | Command |
|---|---|
| Dev (nodemon) | `npm run dev` |
| Compile | `npm run build` |
| Run the compile | `npm start` |

`PORT` defaults to `3000`. CORS allows any origin. The Socket.IO buffer limit is 100 MB.

`GET /` serves `server/public/index.html`. Collaboration itself is Socket.IO, not REST.

### Socket events

Defined in `CCMM/server/src/types/socket.ts` and `CCMM/client/src/types/socket.ts`.

| Event | Role |
|---|---|
| `join-request` | Join with `roomId` and `username`. Rejected with `username-exists` when that name is already in the room. |
| `join-accepted` | Sent to the joiner with their user record and the current member list. |
| `user-joined` | Broadcast to everyone else in the room. |
| `user-disconnected` | Broadcast when a socket disconnects. |
| `sync-file-structure` | Send the file tree, open files, and active file to one socket. |
| `directory-created`, `directory-updated`, `directory-renamed`, `directory-deleted` | Broadcast directory changes to the rest of the room. |
| `file-created`, `file-updated`, `file-renamed`, `file-deleted` | Broadcast file changes to the rest of the room. |
| `send-message` / `receive-message` | Chat. |
| `typing-start` / `typing-pause` | Chat typing state. |
| `user-offline` / `user-online` | Presence. |
| `request-drawing` / `sync-drawing` / `drawing-update` | Shared drawing canvas. |

File edits sync the whole file contents. The server does not store the tree; the clients keep it and broadcast changes.
