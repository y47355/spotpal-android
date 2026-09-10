package com.spotpal.app

import android.app.Application
import com.spotpal.core.network.ApiGraph

/**
 * 搭趣 SpotPal 壳工程 Application：ApiGraph 装配入口（demo 级 ServiceLocator，
 * 生产版迁 Hilt——迁移点唯一：本类 onCreate）。
 */
class SpotPalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiGraph.init(
            baseUrl = BuildConfig.API_BASE,
            wsUrl = BuildConfig.WS_BASE,
        )
    }
}
