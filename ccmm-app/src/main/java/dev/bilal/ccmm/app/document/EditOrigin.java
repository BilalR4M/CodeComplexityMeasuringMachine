package dev.bilal.ccmm.app.document;

/** Who produced a buffer change. REMOTE edits are excluded from local undo. */
public enum EditOrigin {
    LOCAL,
    REMOTE
}
