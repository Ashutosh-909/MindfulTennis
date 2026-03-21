package com.ashutosh.mindfultennis.data.local.db.entity

/**
 * Tracks the synchronization status of a local row with Supabase.
 */
enum class SyncStatus {
    /** Row has been modified locally but not yet pushed to Supabase. */
    PENDING,

    /** Row is in sync with Supabase. */
    SYNCED,

    /** Row was deleted locally; pending deletion on Supabase. */
    PENDING_DELETE,
}
