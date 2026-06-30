package com.quickmemo.demo.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickmemo.demo.core.player.PlaybackState
import com.quickmemo.demo.data.model.Segment
import com.quickmemo.demo.data.repository.RecordingRepository
import com.quickmemo.demo.ui.MainViewModel
import com.quickmemo.demo.ui.theme.BrandOrange
import com.quickmemo.demo.ui.theme.RecordingRed

/**
 * 详情页
 * 
 * 参考讯飞语记布局：
 * - 逐段展示：说话人标签 + 音频波形 + 对应文字
 * - 右下角续录 FAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val recording by viewModel.currentRecording.collectAsState()
    val segments by viewModel.currentSegments.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPosition by viewModel.currentPositionMs.collectAsState()
    val playingIndex by viewModel.playingSegmentIndex.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        recording?.title ?: "录音详情",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 导出 */ }) {
                        Text("📤", fontSize = 18.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            // 续录按钮
            FloatingActionButton(
                onClick = { viewModel.continueRecording() },
                containerColor = BrandOrange,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        if (recording == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("录音不存在或已被删除")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 录音信息卡片
            item {
                RecordingInfoCard(recording = recording!!)
            }

            // 段落标题
            item {
                Text(
                    "录音段落 (${segments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // 逐段展示
            itemsIndexed(segments) { index, segment ->
                SegmentCard(
                    segment = segment,
                    isPlaying = playingIndex == index && playbackState == PlaybackState.PLAYING,
                    isActive = playingIndex == index,
                    currentPosition = if (playingIndex == index) currentPosition else 0,
                    onClick = {
                        if (playingIndex == index && playbackState == PlaybackState.PLAYING) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playSegment(index)
                        }
                    }
                )
            }

            // 重新转译按钮（仅演示）
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* TODO: 重新转译 */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔄", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重新转译（换模型）")
                }
            }

            // 底部留白给 FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * 录音信息卡片
 */
@Composable
private fun RecordingInfoCard(recording: com.quickmemo.demo.data.model.Recording) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                recording.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip("⏱ ${RecordingRepository.formatDuration(recording.totalDurationMs)}")
                InfoChip("👤 ${recording.speakerCount} 位说话人")
                InfoChip("📝 ${recording.segmentCount} 段")
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
}

/**
 * 段落卡片 — 参考讯飞语记样式
 */
@Composable
private fun SegmentCard(
    segment: Segment,
    isPlaying: Boolean,
    isActive: Boolean,
    currentPosition: Long,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) BrandOrange else MaterialTheme.colorScheme.outline,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) BrandOrange.copy(alpha = 0.03f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            // 简化的边框颜色处理
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 说话人标签 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 说话人图标
                    Surface(
                        shape = CircleShape,
                        color = speakerColor(segment.speakerLabel),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                segment.speakerLabel.ifEmpty { "A" },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "说话人${segment.speakerLabel.ifEmpty { "A" }}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 时间范围
                Text(
                    "${RecordingRepository.formatDuration(segment.startOffsetMs)} - ${RecordingRepository.formatDuration(segment.startOffsetMs + segment.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 音频波形（简化版）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isPlaying) BrandOrange.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                // 播放进度条
                if (isActive && segment.durationMs > 0) {
                    val progress = (currentPosition - segment.startOffsetMs).coerceIn(0, segment.durationMs)
                    val fraction = progress.toFloat() / segment.durationMs

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandOrange.copy(alpha = 0.15f))
                    )
                }

                // 播放/暂停图标
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPlaying) "⏸" else "▶",
                        fontSize = 16.sp,
                        color = if (isActive) BrandOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 转译文字区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                if (segment.textContent.isNotEmpty()) {
                    Text(
                        segment.textContent,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                } else {
                    Text(
                        "转译文字将在后台自动生成…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * 说话人颜色映射
 */
private fun speakerColor(label: String): Color {
    return when (label.uppercase()) {
        "A" -> BrandOrange
        "B" -> Color(0xFF5C6BC0)
        "C" -> Color(0xFF26A69A)
        "D" -> Color(0xFFAB47BC)
        else -> Color(0xFF78909C)
    }
}
