package com.glassplayer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.data.Song

@Composable
fun QueueScreen(
    queue: List<Song>,
    currentIndex: Int,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back to player", onBack, size = 42.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                GText("Up next", size = 26.sp, weight = FontWeight.ExtraBold)
                GText(queue.size.toString() + " tracks in queue", size = 13.sp, color = Glass.TextLo)
            }
        }

        if (queue.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().glassCard(RoundedCornerShape(24.dp), strength = 1.0f).padding(18.dp),
            ) {
                GText("Queue is empty", size = 17.sp, weight = FontWeight.Bold)
                GText("Play a song or an album to build your queue.", size = 13.sp, color = Glass.TextLo, maxLines = 3)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                itemsIndexed(queue, key = { index, song -> index.toString() + "-" + song.id }) { index, song ->
                    Row(
                        Modifier.fillMaxWidth()
                            .then(if (index == currentIndex) Modifier.glassCard(RoundedCornerShape(18.dp), Glass.Aqua, 1.2f) else Modifier)
                            .clickable { onPlay(index) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Artwork(song, Modifier.size(42.dp), RoundedCornerShape(12.dp))
                        Column(Modifier.weight(1f)) {
                            GText(song.title, size = 13.sp, weight = FontWeight.SemiBold, color = if (index == currentIndex) Glass.Aqua else Glass.TextHi)
                            GText(
                                if (index == currentIndex) "Now playing · " + song.artist else song.artist,
                                size = 11.sp,
                                color = Glass.TextLo,
                            )
                        }
                        IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up", tint = if (index > 0) Glass.TextHi else Glass.TextLo.copy(alpha = 0.3f))
                        }
                        IconButton(onClick = { onMove(index, index + 1) }, enabled = index < queue.lastIndex, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down", tint = if (index < queue.lastIndex) Glass.TextHi else Glass.TextLo.copy(alpha = 0.3f))
                        }
                        IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove from queue", tint = Color(0xFFFF8A9B))
                        }
                    }
                }
            }
        }
    }
}