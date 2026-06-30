package com.quickmemo.demo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickmemo.demo.core.recorder.RecorderState
import com.quickmemo.demo.data.model.Recording
import com.quickmemo.demo.data.model.TranscriptionStatus
import com.quickmemo.demo.data.repository.RecordingRepository
import com.quickmemo.demo.ui.MainViewModel
import com.quickmemo.demo.ui.theme.BrandOrange
import com.quickmemo.demo.ui.theme.RecordingRed

/**
 * 首页
 * 
 * 核心布局：
 * - 顶部 Logo/名称
 * - 中间大录制按钮（永远可见）
 * - 下方录音列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    hasRecordPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val recordings by viewModel.recordings.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "⚡ 闪电速记",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (recordings.isEmpty() && recorderState == RecorderState.IDLE) {
                // 空状态
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    onPress = {
                        if (hasRecordPermission) viewModel.startRecording()
                        else onRequestPermission()
                    },
                    onRelease = { holdMs ->
                        if (holdMs >= 300) viewModel.stopRecording()
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 12.dp, bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 列表标题
                    item {
                        Text(
                            "最近录音",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(recordings, key = { it.id }) { recording ->
                        RecordingCard(
                            recording = recording,
                            onClick = { viewModel.openDetail(recording) },
                            onDelete = { viewModel.deleteRecording(recording.id) }
                        )
                    }
                }
            }

            // 录制按钮 — 浮动在底部中央
            if (recorderState == RecorderState.IDLE) {
                FloatingRecordButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    hasPermission = hasRecordPermission,
                    onPress = {
                        if (hasRecordPermission) viewModel.startRecording()
                        else onRequestPermission()
                    },
                    onRelease = { holdMs ->
                        if (holdMs >= 300) viewModel.stopRecording()
                    }
                )
            }
        }
    }
}

/**
 * 空状态引导
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: (Long) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎙️", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "按下按钮，开始第一次录音吧",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            // 空状态下也显示录制按钮
            RecordButtonLarge(onPress = onPress, onRelease = onRelease)
        }
    }
}

/**
 * 浮动录制按钮（首页底部）
 */
@Composable
private fun FloatingRecordButton(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    onPress: () -> Unit,
    onRelease: (Long) -> Unit
) {
    if (!hasPermission) {
        // 权限未授权：显示授权引导按钮
        Button(
            onClick = onPress,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("🎤", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("授予麦克风权限")
        }
    } else {
        RecordButtonLarge(
            onPress = onPress,
            onRelease = onRelease,
            modifier = modifier
        )
    }
}

/**
 * 大录制按钮
 */
@Composable
fun RecordButtonLarge(
    onPress: () -> Unit,
    onRelease: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition()
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .scale(breathScale)
            .shadow(8.dp, CircleShape)
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        BrandOrange,
                        Color(0xFFE55A2B)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val startTime = System.currentTimeMillis()
                        onPress()
                        tryAwaitRelease()
                        val holdDuration = System.currentTimeMillis() - startTime
                        onRelease(holdDuration)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🎤",
            fontSize = 32.sp,
            color = Color.White
        )
    }
}

/**
 * 录音卡片
 */
@Composable
fun RecordingCard(
    recording: Recording,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandOrange.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📻", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (recording.transcriptionStatus) {
                        TranscriptionStatus.COMPLETED -> "已转译 · 说话人 ${recording.speakerCount}人"
                        TranscriptionStatus.PROCESSING -> "转译中…"
                        TranscriptionStatus.FAILED -> "转译失败 · 点击重试"
                        TranscriptionStatus.PENDING -> "${recording.segmentCount} 段 · 共 ${RecordingRepository.formatDuration(recording.totalDurationMs)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 右侧信息
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = RecordingRepository.formatDuration(recording.totalDurationMs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (recording.speakerCount > 1) {
                    Text(
                        text = "👤 ${recording.speakerCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    // 长按删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除录音") },
            text = { Text("确定要删除「${recording.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RecordingRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
