package com.quickmemo.demo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickmemo.demo.core.player.AudioPlayer
import com.quickmemo.demo.core.player.PlaybackState
import com.quickmemo.demo.core.recorder.AudioRecorder
import com.quickmemo.demo.core.recorder.RecorderState
import com.quickmemo.demo.data.model.Recording
import com.quickmemo.demo.data.model.Segment
import com.quickmemo.demo.data.repository.RecordingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val audioRecorder = AudioRecorder(application)
    val audioPlayer = AudioPlayer(application)
    val repository = RecordingRepository()

    // === 录制状态 ===
    val recorderState: StateFlow<RecorderState> = audioRecorder.state
    val elapsedMs: StateFlow<Long> = audioRecorder.elapsedMs
    val amplitude: StateFlow<Float> = audioRecorder.amplitude

    // === 播放状态 ===
    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState
    val currentPositionMs: StateFlow<Long> = audioPlayer.currentPositionMs

    // === 数据 ===
    val recordings: StateFlow<List<Recording>> = repository.recordings

    private val _currentRecording = MutableStateFlow<Recording?>(null)
    val currentRecording: StateFlow<Recording?> = _currentRecording.asStateFlow()

    private val _currentSegments = MutableStateFlow<List<Segment>>(emptyList())
    val currentSegments: StateFlow<List<Segment>> = _currentSegments.asStateFlow()

    private val _playingSegmentIndex = MutableStateFlow(-1)
    val playingSegmentIndex: StateFlow<Int> = _playingSegmentIndex.asStateFlow()

    /**
     * 开始录制
     */
    fun startRecording() {
        try {
            audioRecorder.startRecording()
        } catch (e: Exception) {
            // 录制失败，由 UI 层处理
        }
    }

    /**
     * 停止录制并保存
     */
    fun stopRecording() {
        val result = audioRecorder.stopRecording()

        // 丢弃过短的录音
        if (result.durationMs < 1000) {
            java.io.File(result.filePath).delete()
            return
        }

        // 简单模拟说话人数量（基于录音时长）
        val speakerCount = if (result.durationMs > 30000) 2 else 1

        repository.saveRecording(
            audioFilePath = result.filePath,
            durationMs = result.durationMs,
            speakerCount = speakerCount
        )
    }

    /**
     * 取消录制
     */
    fun cancelRecording() {
        audioRecorder.cancelRecording()
    }

    /**
     * 进入详情页
     */
    fun openDetail(recording: Recording) {
        _currentRecording.value = recording
        _currentSegments.value = repository.getSegments(recording.id)
    }

    /**
     * 播放指定段落
     */
    fun playSegment(index: Int) {
        val segments = _currentSegments.value
        if (index !in segments.indices) return

        val segment = segments[index]
        val recording = _currentRecording.value ?: return

        _playingSegmentIndex.value = index
        audioPlayer.play(
            filePath = segment.audioFilePath.ifEmpty { recording.audioFilePath },
            startOffsetMs = segment.startOffsetMs,
            durationMs = segment.durationMs
        )
    }

    /**
     * 暂停/恢复播放
     */
    fun togglePlayPause() {
        when (audioPlayer.playbackState.value) {
            PlaybackState.PLAYING -> audioPlayer.pause()
            PlaybackState.PAUSED -> audioPlayer.resume()
            else -> {}
        }
    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        audioPlayer.stop()
        _playingSegmentIndex.value = -1
    }

    /**
     * 续录（模拟：创建新段落追加到当前录音）
     */
    fun continueRecording() {
        val recording = _currentRecording.value ?: return
        try {
            val filePath = audioRecorder.startRecording()
            // 模拟续录 3 秒后停止
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                val result = audioRecorder.stopRecording()
                if (result.durationMs >= 1000) {
                    repository.appendSegment(
                        recordingId = recording.id,
                        audioFilePath = result.filePath,
                        durationMs = result.durationMs,
                        speakerLabel = "B"
                    )
                    // 刷新
                    _currentRecording.value = repository.getRecording(recording.id)
                    _currentSegments.value = repository.getSegments(recording.id)
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * 删除录音
     */
    fun deleteRecording(recordingId: String) {
        repository.deleteRecording(recordingId)
        _currentRecording.value = null
        _currentSegments.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        audioPlayer.release()
    }
}
