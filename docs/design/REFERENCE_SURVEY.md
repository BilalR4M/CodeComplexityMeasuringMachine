# Reference survey

Retrieved 2026-10-14. Patterns are paraphrased. No screenshots and no copied prose. Each row names the decision it informs.

## Re-checked from the brief

### VS Code

Sources: [User interface](https://code.visualstudio.com/docs/editing/userinterface) and [Custom layout](https://code.visualstudio.com/docs/configure/custom-layout), retrieved 2026-10-14. Status-bar guidance: [Status Bar UX](https://code.visualstudio.com/api/ux-guidelines/status-bar).

Borrow: six regions (activity bar, primary side bar, editor groups, panel, status bar, and an optional secondary side bar). The activity bar switches the side bar. The editor keeps the largest area. Status items are short; workspace items sit on the left, file items on the right. Quick open and the command palette are the keyboard front door.

Avoid: an extension marketplace, webviews, and a secondary side bar in v1. One editor group is enough until tabs exist.

Decision: E1 copies the region model. The inspector is the secondary side bar and starts collapsed below 1100 px.

### JetBrains IDEs

Sources: [Source code navigation](https://www.jetbrains.com/help/idea/navigating-through-the-source-code.html) and [File structure](https://www.jetbrains.com/help/idea/viewing-structure-of-a-source-file.html), retrieved 2026-10-14. Gutter markers are the same family's way of pointing at a problem without opening a dialog; the structure page is the outline pattern this spec copies.

Borrow: a structure view of the current file, sorted and filtered; gutter markers that jump to the explanation; method-level hints rather than a single file-level number.

Avoid: modal inspection dialogs and a toolbar row for every action. JavaFX only has regular and bold weights, so hints stay in the gutter chip and the inspector, not a stack of inlay fonts.

Decision: E3-T04 is the structure view. E5-T01 is the gutter chip. The inspector opens on click, not on hover.

### Zed

Sources: [Channels](https://zed.dev/docs/collaboration/channels) and [Collaboration](https://zed.dev/docs/collaboration/overview), retrieved 2026-10-14. The brief's `.html` URL now resolves without the suffix.

Borrow: avatars in the title bar; click an avatar to follow; following tracks caret, scroll, and file changes inside one pane; moving the caret or editing stops following. Guests can be read-only. The product says, in its own words' place, that sharing a project exposes the host's files to people you trust.

Avoid: requiring sign-in and voice before co-editing is useful. CCMM already has an invite token (ADR-0006). Channels that outlive a session are post-v1.

Decision: E7-T04 follow stops on local navigation and shows a chip. E7 does not add audio. ADR-0011 stays on local checkouts rather than Zed's host filesystem.

### VS Live Share

Sources: [Share and join in VS Code](https://learn.microsoft.com/en-us/visualstudio/liveshare/use/share-project-join-session-visual-studio-code), [Follow and focus](https://learn.microsoft.com/en-us/visualstudio/liveshare/use/coedit-follow-focus-visual-studio), and [Security](https://learn.microsoft.com/en-us/visualstudio/liveshare/reference/security), retrieved 2026-10-14.

Borrow: host approval, a read-only session toggle, and hiding `.gitignore` entries from guests by default. Follow ends when the guest edits, moves the caret, or opens another file. Anonymous guests, when allowed, are read-only until the host approves them.

Avoid: a shared terminal that guests can write, and sharing files outside the project. CCMM already rejects paths that leave the root.

Decision: E7-T10 approval defaults on. E7-T11 hides gitignored and secret files. E6's terminal, if the owner keeps it, is local-only.

### JetBrains Code With Me

Source: [Sunsetting Code With Me](https://blog.jetbrains.com/platform/2026/03/sunsetting-code-with-me/), 2026-03, retrieved 2026-10-14. 2026.1 is the last IDE release that supports it. The plugin gets security updates until the public relays shut down in Q1 2027. The on-premises note (26 March 2026) says the enterprise server is no longer supported in JetBrains IDE Services.

Borrow: the lesson, not the UI. A mainstream IDE is leaving pair programming because usage fell after the pandemic and the cost stayed high. CCMM's reason to keep collaboration is the metrics lens, not a generic shared cursor.

Avoid: a second hosted relay. ADR-0005 stands.

Decision: E7 ships on the operator-run relay. Session summary (E7-T09) is the artifact that outlives the call.

## Speed editors

### Sublime Text

Source: [Projects](https://www.sublimetext.com/docs/projects.html), retrieved 2026-10-14. Goto Anything is the quick-open panel; the projects page is where file and folder patterns keep that panel off binary and ignored files.

Borrow: one shortcut opens a ranked file list as you type; a second gesture jumps to a symbol. The window stays responsive on large files because indexing is not on the keystroke.

Avoid: copying the command set. CCMM quick open is files plus Java symbols only.

Decision: E3-T01 ranks project-relative paths on a background index and never walks `node_modules`. The `heliogrid` tree (31,847 nodes, 3,257 ms) is why the index is ignore-aware.

### Lapce

Source: [lapce/lapce](https://github.com/lapce/lapce) README (rope edits, GPU drawing), retrieved 2026-10-14. Counter-example: [issue 3570](https://github.com/lapce/lapce/issues/3570) (2024-10-11), a 4,000-line scroll that stutters.

Borrow: treat the text buffer as a rope-like structure if a component bake-off needs a third option. Measure scroll, do not trust a toolkit slogan.

Avoid: adopting Lapce or wgpu. The IDE stays JavaFX.

Decision: E0-T01 records scroll smoothness on a 5 MB file. A faster toolkit is not a candidate unless it is embeddable in the JavaFX scene, which Lapce is not.

### Helix

Source: [Helix](https://docs.helix-editor.com/), retrieved 2026-10-14. Counter-example: [issue 15619](https://github.com/helix-editor/helix/issues/15619), insert-mode lag traced to tree-sitter parsing.

Borrow: syntax and diagnostics stay off the input path. Modal editing is out of scope.

Avoid: a native tree-sitter grammar on the JavaFX thread. The Helix report is a parse stall, not a drawing stall.

Decision: ADR-0009 recommends cancellable lexers off-thread. Tree-sitter is a spike candidate only if it cannot block a keystroke.

## Anchored review threads

### GitHub pull request comments

Source: [Commenting on a pull request](https://docs.github.com/en/pull-requests/how-tos/review-pull-requests/commenting-on-a-pull-request), retrieved 2026-10-14.

Borrow: a comment targets a line or a dragged line range; threads are open, resolved, or outdated; a conversations list jumps to each one. A suggested edit is optional and is not v1.

Avoid: hosting the thread on a diff that disappears when the file is edited. CCMM threads live in the session op stream so they move with the text.

Decision: E7-T07 stores an anchor (path + offset range + method name when the range is a method) and transforms it with the same OT as the text. The Collaboration view lists open and resolved threads.

### Reviewable

Source: [Reviewable docs](https://docs.reviewable.io/), retrieved 2026-10-14.

Borrow: a review has an explicit disposition (comment, approve, request changes are GitHub's; Reviewable adds a persistent discussion that stays next to the current revision instead of going stale on the first push). Discussions can be resolved without deleting them.

Avoid: a second account system. CCMM threads are session-scoped and exported to Markdown (E7-T09) so they can be pasted onto a PR.

Decision: resolving a thread keeps it in the list, dimmed. It does not delete the anchor. Outdated means the range was deleted, which the UI must say.

## What this survey refuses

No product screenshot is stored in the repo. No sentence is copied from the pages above. The brief's Zed URL was updated to the path that resolved on this date.
