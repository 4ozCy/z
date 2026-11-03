package com.glassplayer.app.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    /** 0 when the file has no real album, so unrelated songs are never lumped together. */
    val albumId: Long,
    val track: Int,
    val durationMs: Long,
    val uri: Uri,
)
