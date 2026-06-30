package com.quickmemo.demo.data.repository

import com.quickmemo.demo.data.model.Recording
import com.quickmemo.demo.data.model.Segment
import com.quickmemo.demo.data.model.TranscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 录音数据仓库
 * 
 * MVP 版本使用内存存储，后续替换为 Room 数据库
 */
class RecordingRepository {

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    private val _segments = MutableStateFlow<Map<String, List<Segment>>>(emptyMap())
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 保存一条新录音
     */
    fun saveRecording(
        audioFilePath: String,
        durationMs: Long,
        segmentCount: Int = 1,
        speakerCount: Int = 1
    ): Recording {
        val recording = Recording(
            title = dateFormat.format(Date()),
            totalDurationMs = durationMs,
            segmentCount = segmentCount,
            speakerCount = speakerCount,
            audioFilePath = audioFilePath,
            transcriptionStatus = TranscriptionStatus.PENDING
        )

        // 创建默认段落
        val segments = listOf(
            Segment(
                recordingId = recording.id,
                speakerLabel = if (speakerCount > 1) "A" else "",
                audioFilePath = audioFilePath,
                startOffsetMs = 0,
                durationMs = durationMs
            )
        )

        _recordings.value = listOf(recording) + _recordings.value
        _segments.value = _segments.value + (recording.id to segments)

        return recording
    }

    /**
     * 追加续录段落
     */
    fun appendSegment(recordingId: String, audioFilePath: String, durationMs: Long, speakerLabel: String): Recording? {
        val recording = _recordings.value.find { it.id == recordingId } ?: return null
        val existingSegments = _segments.value[recordingId] ?: emptyList()

        val lastOffset = existingSegments.lastOrNull()?.let { it.startOffsetMs + it.durationMs } ?: 0L

        val newSegment = Segment(
            recordingId = recordingId,
            speakerLabel = speakerLabel,
            audioFilePath = audioFilePath,
            startOffsetMs = lastOffset,
            durationMs = durationMs
        )

        _segments.value = _segments.value + (recordingId to (existingSegments + newSegment))

        val updatedRecording = recording.copy(
            totalDurationMs = recording.totalDurationMs + durationMs,
            segmentCount = existingSegments.size + 1,
            speakerCount = (existingSegments + newSegment).map { it.speakerLabel }.distinct().size,
            updatedAt = System.currentTimeMillis()
        )

        _recordings.value = _recordings.value.map { if (it.id == recordingId) updatedRecording else it }

        return updatedRecording
    }

    /**
     * 获取某条录音的所有段落
     */
    fun getSegments(recordingId: String): List<Segment> {
        return _segments.value[recordingId] ?: emptyList()
    }

    /**
     * 获取录音详情
     */
    fun getRecording(recordingId: String): Recording? {
        return _recordings.value.find { it.id == recordingId }
    }

    /**
     * 删除录音
     */
    fun deleteRecording(recordingId: String) {
        _recordings.value = _recordings.value.filter { it.id != recordingId }
        _segments.value = _segments.value - recordingId
    }

    /**
     * 格式化时长为 MM:SS
     */
    companion object {
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
