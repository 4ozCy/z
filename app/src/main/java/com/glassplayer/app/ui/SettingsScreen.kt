package com.glassplayer.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.glassplayer.app.data.Song

@Composable
fun SettingsScreen(
    songCount: Int,
    sortMode: Int,
    currentSong: Song?,
    wallpaperUri: Uri?,
    onSortModeChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRenameCurrent: () -> Unit,
    onDeleteCurrent: () -> Unit,
    onExtractAudio: (String) -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    pad: androidx.compose.foundation.layout.PaddingValues,
) {
    var audioUrl by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(pad),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader("Options", "Make Zmusic feel like yours")

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(26.dp), strength = 1.1f).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GText("Library", size = 19.sp, weight = FontWeight.Bold)
            GText("$songCount songs on this device", size = 14.sp, color = Glass.TextLo)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Title", "Artist", "Album").forEachIndexed { index, label ->
                    GlassPillButton(
                        text = label,
                        icon = if (sortMode == index) Icons.Rounded.Check else Icons.Rounded.SortByAlpha,
                        onClick = { onSortModeChange(index) },
                        tint = if (sortMode == index) Glass.Aqua else Color.White,
                    )
                }
            }
            GText("Sort songs by", size = 13.sp, color = Glass.TextLo)
            GlassPillButton("Rescan music", Icons.Rounded.Refresh, onRefresh, tint = Glass.Violet)
            if (currentSong != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPillButton("Rename file", Icons.Rounded.Edit, onRenameCurrent, tint = Glass.Coral)
                    GlassPillButton("Delete current", Icons.Rounded.Delete, onDeleteCurrent, tint = Glass.Coral)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(26.dp), strength = 1.1f).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GText("Background", size = 19.sp, weight = FontWeight.Bold)
            GText(
                if (wallpaperUri == null) "Use a photo from your device as the app wallpaper."
                else "A custom wallpaper is active.",
                size = 14.sp, color = Glass.TextLo,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPillButton("Choose wallpaper", Icons.Rounded.Image, onPickWallpaper, tint = Glass.Aqua)
                if (wallpaperUri != null) {
                    GlassPillButton("Remove", Icons.Rounded.Delete, onClearWallpaper, tint = Glass.Coral)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(26.dp), strength = 1.1f).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GText("Audio extraction", size = 19.sp, weight = FontWeight.Bold)
            GText("Extract audio as MP3 with yt-dlp. Requires Termux with yt-dlp and FFmpeg installed.", size = 14.sp, color = Glass.TextLo)
            OutlinedTextField(
                value = audioUrl,
                onValueChange = { audioUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Video or media URL") },
                placeholder = { Text("https://...") },
            )
            GlassPillButton(
                "Extract MP3",
                Icons.Rounded.Download,
                onClick = { onExtractAudio(audioUrl) },
                tint = Glass.Aqua,
            )
            GText(
                "Setup in Termux: install yt-dlp and ffmpeg, run termux-setup-storage, and allow external commands in Termux settings. Files are saved to Music. Only download content you have permission to use.",
                size = 12.sp,
                color = Glass.TextLo,
                maxLines = 5,
            )
        }

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(26.dp), strength = 1.1f).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GText("About Zmusic", size = 19.sp, weight = FontWeight.Bold)
            GText("A local music player with a little liquid-glass magic.", size = 14.sp, color = Glass.TextLo, maxLines = 2)
            GText("Your music stays on your device.", size = 13.sp, color = Glass.TextLo)
        }
        Spacer(Modifier.height(8.dp))
    }
}
