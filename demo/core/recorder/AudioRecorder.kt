package com.quickmemo.demo.core.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * 录音状态
 */
enum class RecorderState {
    IDLE,       // 空闲
    RECORDING,  // 录制中
    PAUSED      // 暂停
}

/**
 * 音频录制引擎
 * 
 * 使用 MediaRecorder 实现高效录制
 * AAC-LC 44.1kHz 128kbps
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0L
    
    private val _state = MutableStateFlow(RecorderState.IDLE)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null

    /**
     * 开始录制
     * @return 输出文件路径
     */
    @Throws(IOException::class)
    fun startRecording(): String {
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()

        val fileName = "rec_${System.currentTimeMillis()}.aac"
        outputFile = File(recordingsDir, fileName)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setAudioChannels(1)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        startTime = System.currentTimeMillis()
        _state.value = RecorderState.RECORDING

        // 计时器 — 每 50ms 更新
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                _elapsedMs.value = System.currentTimeMillis() - startTime
                delay(50)
            }
        }

        // 振幅采样 — 用于波形显示
        amplitudeJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    val amp = mediaRecorder?.maxAmplitude ?: 0
                    _amplitude.value = (amp / 32768f).coerceIn(0f, 1f)
                } catch (_: Exception) { }
                delay(100)
            }
        }

        return outputFile!!.absolutePath
    }

    /**
     * 停止录制
     * @return 录音时长（毫秒）
     */
    fun stopRecording(): RecordingResult {
        val elapsed = System.currentTimeMillis() - startTime
        
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }

        mediaRecorder = null
        _state.value = RecorderState.IDLE
        _elapsedMs.value = 0L
        _amplitude.value = 0f

        return RecordingResult(
            filePath = outputFile?.absolutePath ?: "",
            durationMs = elapsed
        )
    }

    /**
     * 取消录制（丢弃文件）
     */
    fun cancelRecording() {
        timerJob?.cancel()
        amplitudeJob?.cancel()
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { }

        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
        _state.value = RecorderState.IDLE
        _elapsedMs.value = 0L
        _amplitude.value = 0f
    }

    fun release() {
        cancelRecording()
    }
}

data class RecordingResult(
    val filePath: String,
    val durationMs: Long
)
