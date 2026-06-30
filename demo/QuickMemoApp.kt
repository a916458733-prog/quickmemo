package com.quickmemo.demo

import android.app.Application

/**
 * 闪电速记 Application
 * 
 * 冷启动优化核心：onCreate() 中零操作
 * 所有初始化延迟到首帧绘制后或按需加载
 */
class QuickMemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 故意留空 — 不在此处做任何 I/O、数据库、网络初始化
        // 所有初始化在 MainActivity 首帧后通过 IdleHandler 执行
    }
}
