        when (tab) {
                    0 -> SongsScreen(
                        displaySongs, loaded, ui, accent, songsList, pad,
                        onPlay = vm::playQueue, onShuffle = vm::playShuffled,
                    )
                    1 -> AlbumsScreen(albums, albumsGrid, pad) {
                        detailIsArtist = false
                        detail = it
                    }
                    2 -> ArtistsScreen(artists, artistsList, pad) {
                        detailIsArtist = true
                        detail = it
                    }
                    3 -> SearchScreen(displaySongs, ui, accent, pad, onPlay = vm::playQueue)
                    else -> SettingsScreen(
                        songCount = songs.size,
                        sortMode = sortMode,
                        currentSong = current,
                        wallpaperUri = null,
                        onSortModeChange = {
                            sortMode = it
                            prefs.edit().putInt("sortMode", it).apply()
                        },
                        onRefresh = vm::refresh,
                        onRenameCurrent = {
                            if (current != null) {
                                renameDraft = current.title
                                showRenameDialog = true
                            }
                        },
                        onDeleteCurrent = {
                            if (current != null) {
                                val deleted = deleteCurrentSong(context, current)
                                if (deleted) vm.refresh()
                            }
                        },
                        onExtractAudio = { url -> requestAudioExtraction(context, url) },
                        onPickWallpaper = {},
                        onClearWallpaper = {},
                        pad = pad,
                    )
                }
