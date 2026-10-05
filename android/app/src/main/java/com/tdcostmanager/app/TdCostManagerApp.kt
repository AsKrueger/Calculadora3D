package com.tdcostmanager.app

import android.app.Application
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class TdCostManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkConfig.initialize(this)
    }
}
