package com.glassplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.data.Song

@Composable
fun SettingsScreen(
    songCount: Int,
    sortMode: Int,
    currentSong: Song?,
    onSortModeChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRenameCurrent: () -> Unit,
    onDeleteCurrent: () -> Unit,
    onReplayWelcome: () -> Unit,
    pad: androidx.compose.foundation.layout.PaddingValues,
) {
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
                    GlassPillButton("Rename current", Icons.Rounded.Edit, onRenameCurrent, tint = Glass.Coral)
                    GlassPillButton("Delete current", Icons.Rounded.Delete, onDeleteCurrent, tint = Glass.Coral)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(26.dp), strength = 1.1f).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GText("About Zmusic", size = 19.sp, weight = FontWeight.Bold)
            GText("A local music player with a little liquid-glass magic.", size = 14.sp, color = Glass.TextLo, maxLines = 2)
            GlassPillButton("Replay welcome", Icons.Rounded.WavingHand, onReplayWelcome, tint = Glass.Coral)
            GText("Your music stays on your device.", size = 13.sp, color = Glass.TextLo)
        }
        Spacer(Modifier.height(8.dp))
    }
}
