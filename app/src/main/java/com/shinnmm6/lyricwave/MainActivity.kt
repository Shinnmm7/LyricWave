package com.shinnmm6.lyricwave

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class LyricLine(val timeMs: Long, val text: String)

class MainActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val songs = mutableListOf<Uri>()
    private var lyrics = listOf<LyricLine>()
    private var lastLyricIndex = -1
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var currentLyric: TextView
    private lateinit var nextLyric: TextView
    private lateinit var progress: SeekBar
    private lateinit var elapsed: TextView
    private lateinit var duration: TextView
    private lateinit var playPause: Button
    private lateinit var vinyl: TextView

    private val musicPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        songs.clear()
        songs.addAll(uris)
        val items = songs.mapIndexed { index, uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            MediaItem.Builder().setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(displayName(uri) ?: "Track ${index + 1}").build())
                .build()
        }
        player.setMediaItems(items)
        player.prepare()
        player.playWhenReady = true
        updateSongInfo()
    }

    private val lyricsPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                contentResolver.openInputStream(uri)?.use { input ->
                    lyrics = parseLrc(BufferedReader(InputStreamReader(input)).readLines().joinToString("\n"))
                }
                lastLyricIndex = -1
                updateLyrics()
                if (lyrics.isEmpty()) currentLyric.text = "No timed lyrics found in this .lrc file"
            } catch (_: Exception) {
                currentLyric.text = "Couldn't read this lyrics file"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        titleView = findViewById(R.id.songTitle)
        artistView = findViewById(R.id.artist)
        currentLyric = findViewById(R.id.currentLyric)
        nextLyric = findViewById(R.id.nextLyric)
        progress = findViewById(R.id.progress)
        elapsed = findViewById(R.id.elapsed)
        duration = findViewById(R.id.duration)
        playPause = findViewById(R.id.playPause)
        vinyl = findViewById(R.id.vinyl)

        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playPause.text = if (isPlaying) "Ⅱ" else "▶"
                if (isPlaying) startVinylAnimation() else vinyl.clearAnimation()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateSongInfo()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateSongInfo()
            }
        })

        findViewById<Button>(R.id.pickMusic).setOnClickListener { musicPicker.launch(arrayOf("audio/*")) }
        findViewById<Button>(R.id.pickLyrics).setOnClickListener { lyricsPicker.launch(arrayOf("text/*", "application/octet-stream")) }
        playPause.setOnClickListener {
            if (player.isPlaying) player.pause()
            else if (player.mediaItemCount > 0) player.play()
        }
        findViewById<Button>(R.id.previous).setOnClickListener { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() }
        findViewById<Button>(R.id.next).setOnClickListener { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
        progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { player.seekTo(seekBar?.progress?.toLong() ?: 0L) }
        })
        handler.post(updateRunnable)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (::player.isInitialized) {
                val pos = player.currentPosition.coerceAtLeast(0)
                val dur = player.duration.takeIf { it > 0 } ?: 0L
                progress.max = dur.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                progress.progress = pos.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                elapsed.text = formatTime(pos)
                duration.text = formatTime(dur)
                updateLyrics()
            }
            handler.postDelayed(this, 250)
        }
    }

    private fun updateLyrics() {
        if (lyrics.isEmpty()) return
        val index = lyrics.indexOfLast { it.timeMs <= player.currentPosition }
        if (index != lastLyricIndex) {
            lastLyricIndex = index
            currentLyric.text = lyrics.getOrNull(index)?.text ?: "♪"
            nextLyric.text = lyrics.getOrNull(index + 1)?.text ?: ""
            currentLyric.startAnimation(AlphaAnimation(0.2f, 1f).apply { duration = 420; fillAfter = true })
        }
    }

    private fun updateSongInfo() {
        val item = player.currentMediaItem
        titleView.text = item?.mediaMetadata?.title ?: "Choose a song"
        artistView.text = item?.mediaMetadata?.artist ?: "Local music"
    }

    private fun startVinylAnimation() {
        val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            .apply { duration = 9000; repeatCount = Animation.INFINITE; interpolator = android.view.animation.LinearInterpolator() }
        vinyl.startAnimation(rotate)
    }

    private fun displayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun parseLrc(raw: String): List<LyricLine> {
        val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")
        return raw.lineSequence().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).ifEmpty { "000" }.toLong()
            LyricLine((min * 60 + sec) * 1000 + fraction, match.groupValues[4].trim())
        }.sortedBy { it.timeMs }.toList()
    }

    private fun formatTime(ms: Long): String = String.format(Locale.US, "%d:%02d", ms / 60000, (ms / 1000) % 60)

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        mediaSession?.release()
        player.release()
        super.onDestroy()
    }
}
