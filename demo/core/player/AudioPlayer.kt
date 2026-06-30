package com.quickmemo.demo.core.player

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackState {
    IDLE, PLAYING, PAUSED, COMPLETED
}

/**
 * 音频播放器 — 精简版，使用 MediaPlayer
 */
class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private var progressJob: Job? = null

    fun play(filePath: String, startOffsetMs: Long = 0, durationMs: Long = 0) {
        release()

        try {
            player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                seekTo(startOffsetMs.toInt())
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.COMPLETED
                }
                start()
            }
            _playbackState.value = PlaybackState.PLAYING

            progressJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    player?.let {
                        _currentPositionMs.value = it.currentPosition.toLong()
                        if (durationMs > 0 && it.currentPosition >= startOffsetMs + durationMs) {
                            it.pause()
                            _playbackState.value = PlaybackState.COMPLETED
                        }
                    }
                    delay(50)
                }
            }
        } catch (_: Exception) { }
    }

    fun pause() {
        player?.pause()
        _playbackState.value = PlaybackState.PAUSED
    }

    fun resume() {
        player?.start()
        _playbackState.value = PlaybackState.PLAYING
    }

    fun stop() {
        player?.stop()
        _playbackState.value = PlaybackState.IDLE
        _currentPositionMs.value = 0
    }

    fun release() {
        progressJob?.cancel()
        player?.release()
        player = null
        _playbackState.value = PlaybackState.IDLE
    }
}
