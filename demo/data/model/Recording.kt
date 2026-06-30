package com.quickmemo.demo.data.model

import java.util.UUID

/**
 * 录音条目
 */
data class Recording(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",           // 录制时间格式化
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalDurationMs: Long = 0,
    val speakerCount: Int = 0,
    val segmentCount: Int = 0,
    val isCompleted: Boolean = true,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,
    val audioFilePath: String = ""
)

enum class TranscriptionStatus {
    PENDING,      // 待转译
    PROCESSING,   // 转译中
    COMPLETED,    // 已转译
    FAILED        // 转译失败
}

/**
 * 录音段落
 */
data class Segment(
    val id: String = UUID.randomUUID().toString(),
    val recordingId: String,
    val speakerLabel: String = "",    // A/B/C
    val audioFilePath: String = "",
    val startOffsetMs: Long = 0,
    val durationMs: Long = 0,
    val textContent: String = "",     // 转译文字
    val createdAt: Long = System.currentTimeMillis()
)
