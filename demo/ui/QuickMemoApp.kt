package com.quickmemo.demo.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickmemo.demo.core.recorder.RecorderState
import com.quickmemo.demo.ui.screens.DetailScreen
import com.quickmemo.demo.ui.screens.HomeScreen

/**
 * 应用主入口
 * 
 * 极简导航：只有两个核心页面
 * 1. 首页（录制按钮 + 列表）
 * 2. 详情页（逐段播放 + 续录）
 */
@Composable
fun QuickMemoApp(
    hasRecordPermission: Boolean,
    onRequestPermission: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val recorderState by viewModel.recorderState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()

    // 当用户点击卡片时切换到详情页
    LaunchedEffect(currentRecording) {
        if (currentRecording != null) {
            currentScreen = Screen.Detail
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == Screen.Detail) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                }
            }
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    hasRecordPermission = hasRecordPermission,
                    onRequestPermission = onRequestPermission
                )
                Screen.Detail -> DetailScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.stopPlayback()
                        currentScreen = Screen.Home
                    }
                )
            }
        }

        // 录制中遮罩 — 覆盖在所有页面之上
        if (recorderState == RecorderState.RECORDING) {
            RecordingOverlay(viewModel = viewModel)
        }
    }
}

private enum class Screen {
    Home, Detail
}
