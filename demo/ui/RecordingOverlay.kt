package com.quickmemo.demo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickmemo.demo.core.recorder.RecorderState
import com.quickmemo.demo.data.repository.RecordingRepository
import com.quickmemo.demo.ui.theme.RecordingRed

/**
 * 录制中全屏遮罩
 * 
 * 覆盖所有页面，确保录制状态始终可见且可操作
 */
@Composable
fun RecordingOverlay(viewModel: MainViewModel) {
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()

    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xDD1A1A1A),
                        Color(0xEE0D0D0D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 计时器
            Text(
                text = RecordingRepository.formatDuration(elapsedMs),
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Thin,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // 波形可视化（简化版：根据振幅变化）
            WaveformVisualizer(
                amplitude = amplitude,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(40.dp)
                    .padding(bottom = 48.dp)
            )

            // 停止按钮 — 脉冲红色，点击停止
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(RecordingRed)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { viewModel.stopRecording() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "停止",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "再次点击停止录制",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 操作提示
            Row(
                horizontalArrangement = Arrangement.spacedBy(64.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✕", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("取消", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔒", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("锁定", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * 简易波形可视化
 * 根据实时振幅绘制动态波形条
 */
@Composable
fun WaveformVisualizer(amplitude: Float, modifier: Modifier = Modifier) {
    val barCount = 5
    val animatedAmplitudes = remember { mutableStateListOf<Float>() }
    
    // 初始化
    LaunchedEffect(Unit) {
        if (animatedAmplitudes.isEmpty()) {
            repeat(barCount) { animatedAmplitudes.add(0f) }
        }
    }

    // 平滑更新
    LaunchedEffect(amplitude) {
        for (i in 0 until barCount) {
            val variation = (Math.random() * 0.3f).toFloat() - 0.15f
            animatedAmplitudes[i] = (amplitude * 0.7f + variation).coerceIn(0.1f, 1f)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val barHeight by animateFloatAsState(
                targetValue = animatedAmplitudes.getOrElse(i) { 0.2f },
                animationSpec = spring(dampingRatio = 0.5f)
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((barHeight * 40).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RecordingRed,
                                RecordingRed.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
    }
}
